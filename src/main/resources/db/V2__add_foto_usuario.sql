-- Execute manualmente no banco, se necessário.
-- O Hibernate com ddl-auto=update também cria a coluna automaticamente.

ALTER TABLE usuarios ADD COLUMN foto VARCHAR(255) NULL;
