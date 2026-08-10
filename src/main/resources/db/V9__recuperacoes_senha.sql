-- Recuperação de senha por código de 6 dígitos.
-- Com ddl-auto=update o Hibernate também cria/atualiza a tabela a partir da entity.

CREATE TABLE IF NOT EXISTS recuperacoes_senha (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    codigo VARCHAR(255) NOT NULL,
    enviado_em DATETIME(6) NOT NULL,
    CONSTRAINT fk_recuperacoes_senha_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_recuperacoes_senha_usuario_enviado
    ON recuperacoes_senha (usuario_id, enviado_em DESC);
