-- Assinatura pode ser vinculada a um Aplicativo Mobile (além de Site).
-- Valor da assinatura do app é sempre informado manualmente (sem plano padrão).
-- Com ddl-auto=update o Hibernate também cria/atualiza a coluna a partir da entity.

ALTER TABLE assinaturas
    ADD COLUMN aplicativo_mobile_id BIGINT NULL;

CREATE INDEX idx_assinaturas_aplicativo_mobile ON assinaturas (aplicativo_mobile_id);

ALTER TABLE assinaturas
    ADD CONSTRAINT fk_assinaturas_aplicativo_mobile
        FOREIGN KEY (aplicativo_mobile_id) REFERENCES aplicativos_mobile (id);
