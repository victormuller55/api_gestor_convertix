package br.net.convertix.gestor.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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
}
