-- Execute no MySQL de produção se a API falhar na inicialização por schema.
-- Idempotente: pode rodar mesmo se parte já existir.

ALTER TABLE clientes
    ADD COLUMN IF NOT EXISTS asaas_customer_id VARCHAR(255) NULL;

-- Índice único (ignore erro se já existir)
-- CREATE UNIQUE INDEX uk_clientes_asaas_customer_id ON clientes (asaas_customer_id);

ALTER TABLE assinaturas
    MODIFY COLUMN forma_pagamento VARCHAR(50) NULL;

ALTER TABLE pagamentos
    MODIFY COLUMN forma_pagamento VARCHAR(50) NULL;
