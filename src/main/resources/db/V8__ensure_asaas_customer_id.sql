-- Compatível com MySQL 5.7 / 8.0 sem "ADD COLUMN IF NOT EXISTS".
-- Se a coluna já existir, o ALTER vai falhar com "Duplicate column" — nesse caso ignore.

ALTER TABLE clientes
    ADD COLUMN asaas_customer_id VARCHAR(255) NULL;

-- Se o índice já existir, ignore o erro de duplicidade.
CREATE UNIQUE INDEX uk_clientes_asaas_customer_id ON clientes (asaas_customer_id);

ALTER TABLE assinaturas
    MODIFY COLUMN forma_pagamento VARCHAR(50) NULL;

ALTER TABLE pagamentos
    MODIFY COLUMN forma_pagamento VARCHAR(50) NULL;
