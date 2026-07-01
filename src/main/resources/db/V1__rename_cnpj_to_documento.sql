-- Renomeia cnpj para documento (CPF/CNPJ).
-- A aplicação aplica automaticamente via DatabaseSchemaMigrator na subida.
ALTER TABLE clientes CHANGE COLUMN cnpj documento VARCHAR(14) NOT NULL;
