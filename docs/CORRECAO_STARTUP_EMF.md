# Correção da falha de inicialização (EntityManagerFactory / JwtAuthenticationFilter)

**Data:** 24/07/2026

## Sintoma

```
Error creating bean with name 'jwtAuthenticationFilter'
→ Error creating bean with name 'usuarioRepository'
→ Cannot resolve reference to bean 'jpaSharedEM_entityManagerFactory'
```

O Jenkins cortava o log antes do `Caused by:`, escondendo a causa real.

## Causas encontradas

### 1. Causa raiz mascarada (principal)
`JwtAuthenticationFilter` era `@Component` (`OncePerRequestFilter`). O Spring Boot registra isso automaticamente no **container Servlet**.

Durante o start do Tomcat, o filtro era criado **cedo**, puxando `UsuarioRepository` → `EntityManagerFactory`. Se o EMF falhasse por qualquer motivo (schema, conexão, etc.), o erro aparecia no filtro JWT — e o `Caused by` real ficava abaixo (cortado pelo timeout do Jenkins).

**Correção:**
- `FilterRegistrationConfig`: desabilita registro Servlet dos filtros de Security (eles só entram via `SecurityFilterChain`).
- `@Lazy` em `UsuarioRepository` no `JwtAuthenticationFilter`, para não forçar o EMF na criação do filtro.

### 2. Schema / Hibernate em produção
- `application-prod` usava `ddl-auto=validate` por padrão.
- Arquivos `db/V1__...V7__...sql` **não são executados** (não há Flyway/Liquibase).
- Entidade `Cliente` exige coluna `asaas_customer_id`. Se ela não existir, o Hibernate em `validate` derruba o EMF.
- Tabelas financeiras podem existir (criadas por `update` antigo), mas a coluna nova em `clientes` pode faltar.

**Correção:**
- Padrão de produção: `JPA_DDL_AUTO=update` (Hibernate cria/ajusta colunas faltantes no boot).
- `DatabaseSchemaMigrator` ampliado: garante `asaas_customer_id` e `forma_pagamento` nullable.
- Script manual `V8__ensure_asaas_customer_id.sql` disponível.
- `hibernate.auto_quote_keyword=true` (coluna `event` é reservada no MySQL).
- Remoção de `columnDefinition = "TEXT/LONGTEXT"` (podem quebrar `validate`).

### 3. Migrations “fantasma”
Pasta `src/main/resources/db/` só documenta SQL. Sem Flyway, nada roda sozinho. O `DatabaseSchemaMigrator` cobre o essencial em runtime.

## Arquivos alterados

| Arquivo | Mudança |
|---------|---------|
| `FilterRegistrationConfig.java` | **Novo** — desliga registro Servlet dos filtros Security |
| `JwtAuthenticationFilter.java` | `@Lazy` no `UsuarioRepository` |
| `DatabaseSchemaMigrator.java` | Garante `asaas_customer_id` + forma_pagamento NULL |
| `application-prod.properties` | `ddl-auto` padrão `update` + driver + auto_quote |
| `application.properties` | `auto_quote_keyword=true` |
| Entidades financeiras / lead | `columnDefinition` → `length` |
| `WebhookEvento.java` | `event` com length explícito |
| `db/V8__ensure_asaas_customer_id.sql` | Script manual de segurança |

## O que fazer no deploy

1. Commit + push destas correções.
2. Rodar a pipeline Jenkins de novo.
3. Na pipeline, confirme:
   - `-e DB_URL=...`
   - `-e DB_USERNAME=...`
   - `-e DB_PASSWORD=...`
   - `-e JWT_SECRET=...`
   - `-e ASAAS_API_KEY=...`
   - `-e ASAAS_WEBHOOK_TOKEN=...`
   - `-e JPA_DDL_AUTO=update` (ou omita; o padrão agora é `update`)
4. Se ainda falhar, nos logs procure a linha **`Caused by:`** completa (aumente o timeout do monitor de logs para 180s).

## Se quiser garantir o schema manualmente no MySQL

Seu MySQL **não** aceita `ADD COLUMN IF NOT EXISTS`. Use:

```sql
ALTER TABLE clientes
    ADD COLUMN asaas_customer_id VARCHAR(255) NULL;
```

Se der erro de coluna duplicada, a coluna já existe — pode ignorar.

Opcional (índice + forma_pagamento):

```sql
CREATE UNIQUE INDEX uk_clientes_asaas_customer_id ON clientes (asaas_customer_id);

ALTER TABLE assinaturas
    MODIFY COLUMN forma_pagamento VARCHAR(50) NULL;

ALTER TABLE pagamentos
    MODIFY COLUMN forma_pagamento VARCHAR(50) NULL;
```