-- Catálogo de planos comerciais.
-- Relacionamento: planos 1:N assinaturas (plano_id opcional nas assinaturas existentes).
--
-- Endpoints:
--   GET    /api/v1/planos ?query=&tipo=&ativo=&page=&size=
--   POST   /api/v1/planos/novo
--   PUT    /api/v1/planos/alterar-dados?id=
--   DELETE /api/v1/planos/apagar?id=          (204; bloqueia se houver assinatura)
--
-- Autorização: somente ADMIN.
-- Tipo: BIOLINK, LANDING_PAGE, SITE_COMERCIAL, APLICATIVO_MOBILE, OUTRO.
-- Vínculo: SITE, APLICATIVO, NENHUM.

CREATE TABLE IF NOT EXISTS planos (
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
);

CREATE INDEX idx_planos_ativo ON planos (ativo);

ALTER TABLE assinaturas
    ADD COLUMN plano_id BIGINT NULL;

CREATE INDEX idx_assinaturas_plano ON assinaturas (plano_id);

ALTER TABLE assinaturas
    ADD CONSTRAINT fk_assinaturas_plano FOREIGN KEY (plano_id) REFERENCES planos (id);
