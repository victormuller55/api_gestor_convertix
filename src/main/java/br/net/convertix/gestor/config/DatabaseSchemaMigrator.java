package br.net.convertix.gestor.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Migrations idempotentes via JDBC para ambientes onde os arquivos db/V*.sql
 * não são executados automaticamente (Flyway/Liquibase não estão no classpath).
 */
@Slf4j
@Component
@Profile("!test")
@Order(0)
@RequiredArgsConstructor
public class DatabaseSchemaMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        migrarClientesDocumento();
        migrarBioLinkItemIcone();
        garantirColunaAsaasCustomerId();
        garantirFormaPagamentoOpcional("assinaturas");
        garantirFormaPagamentoOpcional("pagamentos");
        garantirTabelaAplicativosMobile();
        garantirColunaAssinaturaAplicativoMobile();
    }

    private void migrarClientesDocumento() {
        if (!tabelaExiste("clientes")) {
            return;
        }

        boolean temCnpj = colunaExiste("clientes", "cnpj");
        boolean temDocumento = colunaExiste("clientes", "documento");

        if (temCnpj && temDocumento) {
            log.info("Corrigindo schema de clientes: copiando dados de cnpj para documento e removendo coluna legada");
            jdbcTemplate.execute(
                    "UPDATE clientes SET documento = cnpj WHERE documento IS NULL OR documento = ''");
            jdbcTemplate.execute("ALTER TABLE clientes DROP COLUMN cnpj");
            return;
        }

        if (temCnpj) {
            log.info("Corrigindo schema de clientes: renomeando coluna cnpj para documento");
            jdbcTemplate.execute(
                    "ALTER TABLE clientes CHANGE COLUMN cnpj documento VARCHAR(14) NOT NULL");
        }
    }

    private void migrarBioLinkItemIcone() {
        if (!tabelaExiste("biolink_items") || !colunaExiste("biolink_items", "icone")) {
            return;
        }

        log.info("Normalizando ícones legados de biolink_items para o enum BioLinkItemIcone");
        jdbcTemplate.execute(
                """
                UPDATE biolink_items
                SET icone = NULL
                WHERE icone IS NOT NULL
                  AND icone NOT IN (
                    'WHATSAPP', 'INSTAGRAM', 'TIKTOK', 'YOUTUBE', 'FACEBOOK', 'LINKEDIN', 'X',
                    'TELEGRAM', 'DISCORD', 'SPOTIFY', 'PINTEREST', 'THREADS', 'SNAPCHAT', 'TWITCH',
                    'GITHUB', 'BEHANCE', 'DRIBBBLE', 'MEDIUM', 'SUBSTACK', 'GOOGLE_MAPS', 'OUTROS'
                  )
                """);
    }

    private void garantirColunaAsaasCustomerId() {
        if (!tabelaExiste("clientes") || colunaExiste("clientes", "asaas_customer_id")) {
            return;
        }

        log.info("Adicionando coluna clientes.asaas_customer_id");
        jdbcTemplate.execute("ALTER TABLE clientes ADD COLUMN asaas_customer_id VARCHAR(255) NULL");

        if (!indiceExiste("clientes", "uk_clientes_asaas_customer_id")) {
            jdbcTemplate.execute(
                    "CREATE UNIQUE INDEX uk_clientes_asaas_customer_id ON clientes (asaas_customer_id)");
        }
    }

    private void garantirFormaPagamentoOpcional(String tabela) {
        if (!tabelaExiste(tabela) || !colunaExiste(tabela, "forma_pagamento")) {
            return;
        }

        Boolean nullable = jdbcTemplate.query(
                """
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = 'forma_pagamento'
                """,
                rs -> rs.next() ? "YES".equalsIgnoreCase(rs.getString(1)) : Boolean.TRUE,
                tabela);

        if (Boolean.FALSE.equals(nullable)) {
            log.info("Alterando {}.forma_pagamento para NULL", tabela);
            jdbcTemplate.execute("ALTER TABLE " + tabela + " MODIFY COLUMN forma_pagamento VARCHAR(50) NULL");
        }
    }

    private void garantirTabelaAplicativosMobile() {
        if (tabelaExiste("aplicativos_mobile") || !tabelaExiste("clientes")) {
            return;
        }

        log.info("Criando tabela aplicativos_mobile");
        jdbcTemplate.execute(
                """
                CREATE TABLE aplicativos_mobile (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    cliente_id BIGINT NOT NULL,
                    nome VARCHAR(150) NOT NULL,
                    descricao VARCHAR(500) NULL,
                    status VARCHAR(50) NOT NULL,
                    package_android VARCHAR(255) NULL,
                    bundle_id_ios VARCHAR(255) NULL,
                    versao_android VARCHAR(50) NULL,
                    versao_ios VARCHAR(50) NULL,
                    url_android VARCHAR(500) NULL,
                    url_ios VARCHAR(500) NULL,
                    icone_url VARCHAR(500) NULL,
                    documento_requisitos_url VARCHAR(500) NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    CONSTRAINT fk_aplicativos_mobile_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id)
                )
                """);

        if (!indiceExiste("aplicativos_mobile", "idx_aplicativos_mobile_cliente")) {
            jdbcTemplate.execute(
                    "CREATE INDEX idx_aplicativos_mobile_cliente ON aplicativos_mobile (cliente_id)");
        }
    }

    private void garantirColunaAssinaturaAplicativoMobile() {
        if (!tabelaExiste("assinaturas") || !tabelaExiste("aplicativos_mobile")) {
            return;
        }

        if (!colunaExiste("assinaturas", "aplicativo_mobile_id")) {
            log.info("Adicionando coluna assinaturas.aplicativo_mobile_id");
            jdbcTemplate.execute("ALTER TABLE assinaturas ADD COLUMN aplicativo_mobile_id BIGINT NULL");
        }

        if (!indiceExiste("assinaturas", "idx_assinaturas_aplicativo_mobile")) {
            jdbcTemplate.execute(
                    "CREATE INDEX idx_assinaturas_aplicativo_mobile ON assinaturas (aplicativo_mobile_id)");
        }

        if (!constraintExiste("assinaturas", "fk_assinaturas_aplicativo_mobile")) {
            log.info("Criando FK fk_assinaturas_aplicativo_mobile");
            jdbcTemplate.execute(
                    """
                    ALTER TABLE assinaturas
                        ADD CONSTRAINT fk_assinaturas_aplicativo_mobile
                            FOREIGN KEY (aplicativo_mobile_id) REFERENCES aplicativos_mobile (id)
                    """);
        }
    }

    private boolean tabelaExiste(String tabela) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """,
                Integer.class,
                tabela);
        return count != null && count > 0;
    }

    private boolean colunaExiste(String tabela, String coluna) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                tabela,
                coluna);
        return count != null && count > 0;
    }

    private boolean indiceExiste(String tabela, String indice) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """,
                Integer.class,
                tabela,
                indice);
        return count != null && count > 0;
    }

    private boolean constraintExiste(String tabela, String constraint) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND constraint_name = ?
                """,
                Integer.class,
                tabela,
                constraint);
        return count != null && count > 0;
    }
}
