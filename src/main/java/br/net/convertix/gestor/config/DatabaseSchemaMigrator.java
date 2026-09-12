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
        garantirTabelaProjetos();
        garantirTabelaPlanos();
        garantirTabelasLandingPages();
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

    private void garantirTabelaProjetos() {
        if (!tabelaExiste("clientes") || !tabelaExiste("sites") || !tabelaExiste("aplicativos_mobile")) {
            return;
        }

        if (!tabelaExiste("projetos")) {
            log.info("Criando tabela projetos");
            jdbcTemplate.execute(
                    """
                    CREATE TABLE projetos (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        cliente_id BIGINT NOT NULL,
                        titulo VARCHAR(150) NOT NULL,
                        tipo VARCHAR(50) NOT NULL,
                        etapa VARCHAR(50) NOT NULL,
                        site_id BIGINT NULL,
                        aplicativo_mobile_id BIGINT NULL,
                        prazo DATE NULL,
                        descricao VARCHAR(2000) NULL,
                        observacao_interna VARCHAR(2000) NULL,
                        created_at DATETIME(6) NOT NULL,
                        updated_at DATETIME(6) NOT NULL,
                        CONSTRAINT fk_projetos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
                        CONSTRAINT fk_projetos_site FOREIGN KEY (site_id) REFERENCES sites (id),
                        CONSTRAINT fk_projetos_aplicativo_mobile FOREIGN KEY (aplicativo_mobile_id) REFERENCES aplicativos_mobile (id)
                    )
                    """);
        }

        if (!indiceExiste("projetos", "idx_projetos_cliente")) {
            jdbcTemplate.execute("CREATE INDEX idx_projetos_cliente ON projetos (cliente_id)");
        }
        if (!indiceExiste("projetos", "idx_projetos_etapa")) {
            jdbcTemplate.execute("CREATE INDEX idx_projetos_etapa ON projetos (etapa)");
        }
        if (!indiceExiste("projetos", "idx_projetos_site")) {
            jdbcTemplate.execute("CREATE INDEX idx_projetos_site ON projetos (site_id)");
        }
        if (!indiceExiste("projetos", "idx_projetos_aplicativo_mobile")) {
            jdbcTemplate.execute(
                    "CREATE INDEX idx_projetos_aplicativo_mobile ON projetos (aplicativo_mobile_id)");
        }

        if (!tabelaExiste("historico_etapa_projetos")) {
            log.info("Criando tabela historico_etapa_projetos");
            jdbcTemplate.execute(
                    """
                    CREATE TABLE historico_etapa_projetos (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        projeto_id BIGINT NOT NULL,
                        etapa_anterior VARCHAR(50) NULL,
                        etapa_nova VARCHAR(50) NOT NULL,
                        origem VARCHAR(50) NOT NULL,
                        mensagem VARCHAR(500) NULL,
                        created_at DATETIME(6) NOT NULL,
                        CONSTRAINT fk_historico_etapa_projetos_projeto FOREIGN KEY (projeto_id) REFERENCES projetos (id)
                    )
                    """);
        }

        if (!indiceExiste("historico_etapa_projetos", "idx_historico_etapa_projetos_projeto")) {
            jdbcTemplate.execute(
                    "CREATE INDEX idx_historico_etapa_projetos_projeto ON historico_etapa_projetos (projeto_id)");
        }
    }

    private void garantirTabelaPlanos() {
        if (!tabelaExiste("planos")) {
            log.info("Criando tabela planos");
            jdbcTemplate.execute(
                    """
                    CREATE TABLE planos (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        codigo VARCHAR(50) NULL,
                        nome VARCHAR(150) NOT NULL,
                        tipo VARCHAR(50) NOT NULL,
                        vinculo VARCHAR(50) NOT NULL,
                        valor DECIMAL(12, 2) NULL,
                        valor_livre TINYINT(1) NOT NULL DEFAULT 0,
                        ciclo VARCHAR(50) NOT NULL,
                        descricao_padrao VARCHAR(255) NULL,
                        ativo TINYINT(1) NOT NULL DEFAULT 1,
                        ordem INT NOT NULL DEFAULT 0,
                        created_at DATETIME(6) NOT NULL,
                        updated_at DATETIME(6) NOT NULL,
                        UNIQUE KEY uk_planos_codigo (codigo)
                    )
                    """);
        }

        if (!indiceExiste("planos", "idx_planos_ativo")) {
            jdbcTemplate.execute("CREATE INDEX idx_planos_ativo ON planos (ativo)");
        }

        if (tabelaExiste("assinaturas") && !colunaExiste("assinaturas", "plano_id")) {
            log.info("Adicionando coluna assinaturas.plano_id");
            jdbcTemplate.execute("ALTER TABLE assinaturas ADD COLUMN plano_id BIGINT NULL");
        }

        if (tabelaExiste("assinaturas") && !indiceExiste("assinaturas", "idx_assinaturas_plano")) {
            jdbcTemplate.execute("CREATE INDEX idx_assinaturas_plano ON assinaturas (plano_id)");
        }

        if (tabelaExiste("assinaturas") && tabelaExiste("planos")
                && !constraintExiste("assinaturas", "fk_assinaturas_plano")) {
            log.info("Criando FK fk_assinaturas_plano");
            jdbcTemplate.execute(
                    """
                    ALTER TABLE assinaturas
                        ADD CONSTRAINT fk_assinaturas_plano
                            FOREIGN KEY (plano_id) REFERENCES planos (id)
                    """);
        }

        seedPlanosIniciais();
    }

    private void seedPlanosIniciais() {
        if (!tabelaExiste("planos")) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM planos", Integer.class);
        if (count != null && count > 0) {
            return;
        }

        log.info("Inserindo planos iniciais do catálogo");
        jdbcTemplate.update(
                """
                INSERT INTO planos (codigo, nome, tipo, vinculo, valor, valor_livre, ciclo, descricao_padrao, ativo, ordem, created_at, updated_at)
                VALUES
                ('biolink', 'BioLink Profissional', 'BIOLINK', 'SITE', 30.00, 0, 'MONTHLY', 'Assinatura mensal BioLink Profissional', 1, 1, NOW(6), NOW(6)),
                ('landing_page', 'Landing Page', 'LANDING_PAGE', 'SITE', 90.00, 0, 'MONTHLY', 'Assinatura mensal Landing Page', 1, 2, NOW(6), NOW(6)),
                ('site_institucional', 'Site Institucional Completo', 'SITE_COMERCIAL', 'SITE', 170.00, 0, 'MONTHLY', 'Assinatura mensal Site Institucional Completo', 1, 3, NOW(6), NOW(6)),
                ('aplicativo_mobile', 'Aplicativo Mobile', 'APLICATIVO_MOBILE', 'APLICATIVO', NULL, 1, 'MONTHLY', '', 1, 4, NOW(6), NOW(6)),
                ('outro', 'Outro valor', 'OUTRO', 'SITE', NULL, 1, 'MONTHLY', '', 1, 5, NOW(6), NOW(6))
                """);
    }

    private void garantirTabelasLandingPages() {
        if (!tabelaExiste("sites")) {
            return;
        }

        if (!tabelaExiste("landing_pages")) {
            log.info("Criando tabela landing_pages");
            jdbcTemplate.execute(
                    """
                    CREATE TABLE landing_pages (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        site_id BIGINT NOT NULL,
                        slug VARCHAR(255) NOT NULL,
                        created_at DATETIME(6) NOT NULL,
                        updated_at DATETIME(6) NOT NULL,
                        UNIQUE KEY idx_landing_pages_slug (slug),
                        UNIQUE KEY idx_landing_pages_site_id (site_id),
                        CONSTRAINT fk_landing_pages_site FOREIGN KEY (site_id) REFERENCES sites (id)
                    )
                    """);
        }

        if (!tabelaExiste("landing_page_formularios")) {
            log.info("Criando tabela landing_page_formularios");
            jdbcTemplate.execute(
                    """
                    CREATE TABLE landing_page_formularios (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        landing_page_id BIGINT NOT NULL,
                        nome VARCHAR(255) NOT NULL,
                        titulo VARCHAR(255) NOT NULL,
                        descricao VARCHAR(500) NULL,
                        texto_botao VARCHAR(255) NOT NULL,
                        ativo TINYINT(1) NOT NULL,
                        created_at DATETIME(6) NOT NULL,
                        updated_at DATETIME(6) NOT NULL,
                        CONSTRAINT fk_landing_page_formularios_lp FOREIGN KEY (landing_page_id) REFERENCES landing_pages (id)
                    )
                    """);
        }

        if (!indiceExiste("landing_page_formularios", "idx_landing_page_formularios_landing_page_id")) {
            jdbcTemplate.execute(
                    "CREATE INDEX idx_landing_page_formularios_landing_page_id ON landing_page_formularios (landing_page_id)");
        }

        if (!tabelaExiste("landing_page_campos")) {
            log.info("Criando tabela landing_page_campos");
            jdbcTemplate.execute(
                    """
                    CREATE TABLE landing_page_campos (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        formulario_id BIGINT NOT NULL,
                        nome_interno VARCHAR(255) NOT NULL,
                        label VARCHAR(255) NOT NULL,
                        tipo VARCHAR(50) NOT NULL,
                        placeholder VARCHAR(255) NULL,
                        obrigatorio TINYINT(1) NOT NULL,
                        ordem INT NOT NULL,
                        ativo TINYINT(1) NOT NULL,
                        created_at DATETIME(6) NOT NULL,
                        updated_at DATETIME(6) NOT NULL,
                        UNIQUE KEY idx_landing_page_campos_formulario_nome_interno (formulario_id, nome_interno),
                        CONSTRAINT fk_landing_page_campos_formulario FOREIGN KEY (formulario_id) REFERENCES landing_page_formularios (id)
                    )
                    """);
        }

        if (!indiceExiste("landing_page_campos", "idx_landing_page_campos_formulario_id")) {
            jdbcTemplate.execute(
                    "CREATE INDEX idx_landing_page_campos_formulario_id ON landing_page_campos (formulario_id)");
        }

        if (!tabelaExiste("landing_page_leads")) {
            log.info("Criando tabela landing_page_leads");
            jdbcTemplate.execute(
                    """
                    CREATE TABLE landing_page_leads (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        landing_page_id BIGINT NOT NULL,
                        formulario_id BIGINT NOT NULL,
                        nome VARCHAR(255) NULL,
                        email VARCHAR(255) NULL,
                        telefone VARCHAR(255) NULL,
                        ip VARCHAR(255) NULL,
                        origem VARCHAR(255) NULL,
                        user_agent VARCHAR(255) NULL,
                        status VARCHAR(50) NOT NULL,
                        observacao VARCHAR(255) NULL,
                        created_at DATETIME(6) NOT NULL,
                        CONSTRAINT fk_landing_page_leads_lp FOREIGN KEY (landing_page_id) REFERENCES landing_pages (id),
                        CONSTRAINT fk_landing_page_leads_formulario FOREIGN KEY (formulario_id) REFERENCES landing_page_formularios (id)
                    )
                    """);
        }

        if (!indiceExiste("landing_page_leads", "idx_landing_page_leads_landing_page_id")) {
            jdbcTemplate.execute(
                    "CREATE INDEX idx_landing_page_leads_landing_page_id ON landing_page_leads (landing_page_id)");
        }
        if (!indiceExiste("landing_page_leads", "idx_landing_page_leads_formulario_id")) {
            jdbcTemplate.execute(
                    "CREATE INDEX idx_landing_page_leads_formulario_id ON landing_page_leads (formulario_id)");
        }
        if (!indiceExiste("landing_page_leads", "idx_landing_page_leads_status")) {
            jdbcTemplate.execute("CREATE INDEX idx_landing_page_leads_status ON landing_page_leads (status)");
        }

        if (!tabelaExiste("landing_page_lead_valores")) {
            log.info("Criando tabela landing_page_lead_valores");
            jdbcTemplate.execute(
                    """
                    CREATE TABLE landing_page_lead_valores (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        lead_id BIGINT NOT NULL,
                        campo_id BIGINT NOT NULL,
                        valor TEXT NOT NULL,
                        CONSTRAINT fk_landing_page_lead_valores_lead FOREIGN KEY (lead_id) REFERENCES landing_page_leads (id),
                        CONSTRAINT fk_landing_page_lead_valores_campo FOREIGN KEY (campo_id) REFERENCES landing_page_campos (id)
                    )
                    """);
        }

        if (!indiceExiste("landing_page_lead_valores", "idx_landing_page_lead_valores_lead_id")) {
            jdbcTemplate.execute(
                    "CREATE INDEX idx_landing_page_lead_valores_lead_id ON landing_page_lead_valores (lead_id)");
        }
        if (!indiceExiste("landing_page_lead_valores", "idx_landing_page_lead_valores_campo_id")) {
            jdbcTemplate.execute(
                    "CREATE INDEX idx_landing_page_lead_valores_campo_id ON landing_page_lead_valores (campo_id)");
        }

        garantirCampoMensagemLandingPages();
    }

    private void garantirCampoMensagemLandingPages() {
        if (!tabelaExiste("landing_page_formularios") || !tabelaExiste("landing_page_campos")) {
            return;
        }

        int inseridos = jdbcTemplate.update(
                """
                INSERT INTO landing_page_campos (
                    formulario_id, nome_interno, label, tipo, placeholder, obrigatorio, ordem, ativo, created_at, updated_at
                )
                SELECT
                    f.id,
                    'mensagem',
                    'Mensagem',
                    'TEXTAREA',
                    'Como podemos ajudar?',
                    0,
                    4,
                    1,
                    NOW(6),
                    NOW(6)
                FROM landing_page_formularios f
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM landing_page_campos c
                    WHERE c.formulario_id = f.id
                      AND c.nome_interno = 'mensagem'
                )
                """);
        if (inseridos > 0) {
            log.info("Campo mensagem adicionado em {} formulário(s) de landing page", inseridos);
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
