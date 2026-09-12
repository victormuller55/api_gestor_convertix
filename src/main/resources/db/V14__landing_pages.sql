-- Módulo Landing Pages + Leads.
-- Relacionamento: sites 1:1 landing_pages; landing_pages 1:N formulários, leads;
--                 formulários 1:N campos; leads 1:N valores.
-- Com ddl-auto=update o Hibernate também cria/atualiza as tabelas a partir das entities.
--
-- Endpoints:
--   GET    /api/v1/landing-pages ?id=&query=&page=&size=
--   POST   /api/v1/landing-pages/novo
--   PUT    /api/v1/landing-pages/alterar-dados?id=
--   DELETE /api/v1/landing-pages/apagar?id=
--   GET    /api/v1/landing-pages/formularios ?landing_page_id=&id=
--   POST   /api/v1/landing-pages/formularios/novo?landing_page_id=
--   PUT    /api/v1/landing-pages/formularios/alterar-dados?landing_page_id=&id=
--   DELETE /api/v1/landing-pages/formularios/apagar?landing_page_id=&id=
--   GET    /api/v1/landing-pages/campos ?formulario_id=
--   POST   /api/v1/landing-pages/campos/novo?formulario_id=
--   PUT    /api/v1/landing-pages/campos/alterar-dados?id=
--   DELETE /api/v1/landing-pages/campos/apagar?id=
--   GET    /api/v1/landing-pages/leads ?landing_page_id=&id=&status=&query=&page=&size=
--   PUT    /api/v1/landing-pages/leads/alterar-status?landing_page_id=&id=
--   DELETE /api/v1/landing-pages/leads/apagar?landing_page_id=&id=
--   GET    /api/v1/landing-pages/publico ?slug=          (público)
--   POST   /api/v1/landing-pages/publico/leads ?slug=    (público)
--
-- Autorização: admin vê tudo; cliente só as suas. Captura de lead é pública.
-- Tipo de site exigido: LANDING_PAGE. Site institucional continua bloqueado.

CREATE TABLE IF NOT EXISTS landing_pages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    site_id BIGINT NOT NULL,
    slug VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY idx_landing_pages_slug (slug),
    UNIQUE KEY idx_landing_pages_site_id (site_id),
    CONSTRAINT fk_landing_pages_site FOREIGN KEY (site_id) REFERENCES sites (id)
);

CREATE TABLE IF NOT EXISTS landing_page_formularios (
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
);

CREATE INDEX idx_landing_page_formularios_landing_page_id ON landing_page_formularios (landing_page_id);

CREATE TABLE IF NOT EXISTS landing_page_campos (
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
);

CREATE INDEX idx_landing_page_campos_formulario_id ON landing_page_campos (formulario_id);

CREATE TABLE IF NOT EXISTS landing_page_leads (
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
);

CREATE INDEX idx_landing_page_leads_landing_page_id ON landing_page_leads (landing_page_id);
CREATE INDEX idx_landing_page_leads_formulario_id ON landing_page_leads (formulario_id);
CREATE INDEX idx_landing_page_leads_status ON landing_page_leads (status);

CREATE TABLE IF NOT EXISTS landing_page_lead_valores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lead_id BIGINT NOT NULL,
    campo_id BIGINT NOT NULL,
    valor TEXT NOT NULL,
    CONSTRAINT fk_landing_page_lead_valores_lead FOREIGN KEY (lead_id) REFERENCES landing_page_leads (id),
    CONSTRAINT fk_landing_page_lead_valores_campo FOREIGN KEY (campo_id) REFERENCES landing_page_campos (id)
);

CREATE INDEX idx_landing_page_lead_valores_lead_id ON landing_page_lead_valores (lead_id);
CREATE INDEX idx_landing_page_lead_valores_campo_id ON landing_page_lead_valores (campo_id);
