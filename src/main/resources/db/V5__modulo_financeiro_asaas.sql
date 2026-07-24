-- Módulo financeiro Asaas
-- Com ddl-auto=update o Hibernate também cria/atualiza as tabelas a partir das entities.

ALTER TABLE clientes
    ADD COLUMN IF NOT EXISTS asaas_customer_id VARCHAR(255) NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_clientes_asaas_customer_id ON clientes (asaas_customer_id);

CREATE TABLE IF NOT EXISTS assinaturas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    site_id BIGINT NULL,
    asaas_subscription_id VARCHAR(255) NULL,
    valor DECIMAL(12, 2) NOT NULL,
    descricao VARCHAR(255) NULL,
    ciclo VARCHAR(50) NOT NULL,
    forma_pagamento VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    proxima_cobranca DATE NULL,
    credit_card_token VARCHAR(255) NULL,
    external_reference VARCHAR(255) NULL,
    mensagem_asaas TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_assinaturas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT fk_assinaturas_site FOREIGN KEY (site_id) REFERENCES sites (id),
    CONSTRAINT uk_assinaturas_asaas_id UNIQUE (asaas_subscription_id)
);

CREATE TABLE IF NOT EXISTS pagamentos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    site_id BIGINT NULL,
    assinatura_id BIGINT NULL,
    asaas_payment_id VARCHAR(255) NULL,
    valor DECIMAL(12, 2) NOT NULL,
    descricao VARCHAR(255) NULL,
    status VARCHAR(50) NOT NULL,
    forma_pagamento VARCHAR(50) NOT NULL,
    parcelas INT NULL,
    qr_code TEXT NULL,
    codigo_pix TEXT NULL,
    invoice_url VARCHAR(500) NULL,
    comprovante_url VARCHAR(500) NULL,
    data_vencimento DATE NULL,
    data_confirmacao DATETIME(6) NULL,
    mensagem_asaas TEXT NULL,
    external_reference VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_pagamentos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT fk_pagamentos_site FOREIGN KEY (site_id) REFERENCES sites (id),
    CONSTRAINT fk_pagamentos_assinatura FOREIGN KEY (assinatura_id) REFERENCES assinaturas (id),
    CONSTRAINT uk_pagamentos_asaas_id UNIQUE (asaas_payment_id)
);

CREATE TABLE IF NOT EXISTS historico_status_pagamentos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pagamento_id BIGINT NOT NULL,
    status_anterior VARCHAR(50) NULL,
    status_novo VARCHAR(50) NOT NULL,
    origem VARCHAR(50) NOT NULL,
    mensagem TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_historico_pagamento FOREIGN KEY (pagamento_id) REFERENCES pagamentos (id)
);

CREATE TABLE IF NOT EXISTS webhook_eventos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(255) NULL,
    event VARCHAR(100) NOT NULL,
    payload LONGTEXT NOT NULL,
    processado BIT(1) NOT NULL,
    mensagem_erro TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_webhook_event_id UNIQUE (event_id)
);

CREATE INDEX idx_pagamentos_cliente_id ON pagamentos (cliente_id);
CREATE INDEX idx_pagamentos_status ON pagamentos (status);
CREATE INDEX idx_pagamentos_forma_pagamento ON pagamentos (forma_pagamento);
CREATE INDEX idx_pagamentos_created_at ON pagamentos (created_at);
CREATE INDEX idx_assinaturas_cliente_id ON assinaturas (cliente_id);
CREATE INDEX idx_assinaturas_status ON assinaturas (status);
