-- Corrige bancos onde o Hibernate criou "documento" mas manteve "cnpj" (NOT NULL).
-- A aplicação executa essa correção automaticamente via DatabaseSchemaMigrator.
-- Use manualmente apenas se necessário:

UPDATE clientes SET documento = cnpj WHERE documento IS NULL OR documento = '';
ALTER TABLE clientes DROP COLUMN cnpj;
