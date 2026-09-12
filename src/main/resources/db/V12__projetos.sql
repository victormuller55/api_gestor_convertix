-- Módulo Projetos
-- Relacionamento: clientes 1:N projetos. Vínculo opcional com 1 site XOR 1 aplicativo_mobile.
-- Com ddl-auto=update o Hibernate também cria/atualiza as tabelas a partir das entities.
--
-- Endpoints (convenção do projeto: ações em português, id em query param):
--   GET    /api/v1/projetos
--          ?id=&query=&cliente_id=&etapa=&tipo=&page=&size=
--   POST   /api/v1/projetos/novo
--   PUT    /api/v1/projetos/alterar-dados?id=
--   PUT    /api/v1/projetos/alterar-etapa?id=
--   DELETE /api/v1/projetos/apagar?id=          (204)
--
-- Payload JSON (snake_case), campos obrigatórios: cliente_id, titulo, tipo, etapa.
-- Opcionais: site_id, aplicativo_mobile_id, prazo, descricao, observacao_interna.
-- Informe somente site_id ou aplicativo_mobile_id.
--
-- Tipo (enum STRING): BIOLINK, LANDING_PAGE, SITE_COMERCIAL, APLICATIVO_MOBILE, OUTRO.
-- Etapa (enum STRING): BRIEFING, EM_ANDAMENTO, HOMOLOGACAO, CONCLUIDO, PAUSADO, CANCELADO.
--
-- Autorização: admin vê tudo e altera; cliente só consulta os seus.

CREATE TABLE IF NOT EXISTS projetos (
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
);

CREATE INDEX idx_projetos_cliente ON projetos (cliente_id);
CREATE INDEX idx_projetos_etapa ON projetos (etapa);
CREATE INDEX idx_projetos_site ON projetos (site_id);
CREATE INDEX idx_projetos_aplicativo_mobile ON projetos (aplicativo_mobile_id);

CREATE TABLE IF NOT EXISTS historico_etapa_projetos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    projeto_id BIGINT NOT NULL,
    etapa_anterior VARCHAR(50) NULL,
    etapa_nova VARCHAR(50) NOT NULL,
    origem VARCHAR(50) NOT NULL,
    mensagem VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_historico_etapa_projetos_projeto FOREIGN KEY (projeto_id) REFERENCES projetos (id)
);

CREATE INDEX idx_historico_etapa_projetos_projeto ON historico_etapa_projetos (projeto_id);
