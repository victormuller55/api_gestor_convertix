-- Módulo Aplicativos Mobile
-- Relacionamento: clientes 1:N aplicativos_mobile (cliente obrigatório).
-- Com ddl-auto=update o Hibernate também cria/atualiza a tabela a partir da entity.
--
-- Endpoints (convenção do projeto: ações em português, id em query param):
--   GET    /api/v1/aplicativos-mobile
--          ?id=&query=&cliente_id=&status=&page=&size=
--   POST   /api/v1/aplicativos-mobile/novo
--   PUT    /api/v1/aplicativos-mobile/alterar-dados?id=
--   DELETE /api/v1/aplicativos-mobile/apagar?id=          (204)
--   POST   /api/v1/aplicativos-mobile/documento-requisitos?id=
--          multipart part "documento" (somente PDF, máx. 5 MB)
--   DELETE /api/v1/aplicativos-mobile/documento-requisitos?id=
--
-- Payload JSON (snake_case), campos obrigatórios: cliente_id, nome, status.
-- Opcionais: descricao, package_android, bundle_id_ios, versao_android, versao_ios,
--            url_android, url_ios, icone_url.
--
-- Status (enum STRING): DESENVOLVIMENTO, HOMOLOGACAO, PRODUCAO, PAUSADO, ENCERRADO.
--
-- Documento de requisitos:
--   - PDF apenas (MIME application/pdf + assinatura %PDF + extensão .pdf)
--   - armazenamento em disco local (app.upload.dir / uploads/aplicativos-mobile/{uuid}.pdf)
--   - referência salva em documento_requisitos_url (ex.: /uploads/aplicativos-mobile/{uuid}.pdf)
--   - limite: spring.servlet.multipart.max-file-size (5 MB)
--   - nome físico = UUID (não usa o nome original do arquivo)
--
-- Autorização: mesmo modelo de Sites (admin vê tudo; cliente só os seus).

CREATE TABLE IF NOT EXISTS aplicativos_mobile (
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
);

CREATE INDEX idx_aplicativos_mobile_cliente ON aplicativos_mobile (cliente_id);
