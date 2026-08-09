# Dashboard da Home — `GET /api/v1/dashboard/inicio`

Documentação para o app Flutter (`web_gestor_site_covertix`) consumir o endpoint único da tela **Início**.

Substitui os vários GETs (sites, biolinks, clientes, usuários, financeiro) usados só para montar contadores por **uma única requisição**.

---

## Endpoint

```
GET /api/v1/dashboard/inicio
```

### Autenticação

```
Authorization: Bearer <JWT>
```

Mesmo padrão das demais rotas autenticadas. Sem token → `401`. Token inválido/expirado → `401`.

### Escopo automático (pelo JWT)

| Tipo do usuário | Escopo (`escopo` na resposta) | Dados retornados |
|---|---|---|
| `ADMIN` | `ADMIN` | Visão global (todos clientes/sites/pagamentos/assinaturas) |
| `CLIENTE` | `CLIENTE` | Apenas do `cliente_id` do usuário logado |

Não é necessário enviar `cliente_id` na query. O backend resolve pelo token.

---

## Query params (todos opcionais)

| Param | Tipo | Default | Min | Max | Uso |
|---|---|---|---|---|---|
| `meses` | int | `12` | `3` | `24` | Janela das séries mensais |
| `limite_atividades` | int | `10` | `1` | `30` | Itens em `atividades_recentes` |
| `limite_alertas` | int | `10` | `1` | `30` | Itens em `alertas` |
| `limite_tops` | int | `5` | `1` | `10` | Itens em `tops.*` |

Valores fora da faixa são **ajustados (clamp)** para o intervalo válido.

### Exemplos

```http
GET /api/v1/dashboard/inicio
Authorization: Bearer eyJhbGciOi...
```

```http
GET /api/v1/dashboard/inicio?meses=6&limite_atividades=15&limite_alertas=10&limite_tops=5
Authorization: Bearer eyJhbGciOi...
```

### Flutter (exemplo)

```dart
final uri = Uri.parse('$baseUrl/api/v1/dashboard/inicio').replace(
  queryParameters: {
    'meses': '12',
    'limite_atividades': '10',
    'limite_alertas': '10',
    'limite_tops': '5',
  },
);

final response = await http.get(
  uri,
  headers: {
    'Authorization': 'Bearer $token',
    'Accept': 'application/json',
  },
);

if (response.statusCode == 200) {
  final data = jsonDecode(response.body) as Map<String, dynamic>;
  // usar data['kpis'], data['series'], etc.
}
```

---

## Convenções da resposta

- JSON em **snake_case**
- Números nunca vêm `null` → usar `0` / `0.0`
- Listas vazias → `[]`
- Strings opcionais podem ser `null` (ex.: `foto`, `cliente_nome`, `label` em alguns itens)
- Datas em **ISO-8601**
  - `gerado_em` → `Instant` UTC (ex.: `2026-08-09T20:00:00Z`)
  - demais timestamps → `LocalDateTime` (ex.: `2026-08-08T10:00:00`)
- Enums como **string** do nome Java (`ATIVO`, `PENDING`, `MONTHLY`, etc.)

---

## Contrato (resumo por bloco)

### Raiz

| Campo | Tipo | Descrição |
|---|---|---|
| `gerado_em` | string (ISO) | Momento da geração no servidor |
| `escopo` | `"ADMIN"` \| `"CLIENTE"` | Escopo efetivo dos dados |
| `periodo_meses` | int | Janela efetiva usada nas séries |
| `usuario` | object | Resumo do usuário logado |
| `kpis` | object | Contadores / métricas principais |
| `distribuicoes` | object | Breakdowns para gráficos de pizza/barra |
| `series` | object | Séries mensais (linha/área) |
| `funil` | object | Funil comercial + taxas |
| `alertas` | array | Avisos priorizados |
| `tops` | object | Rankings / recentes |
| `assinatura_destaque` | object | Assinatura ACTIVE em destaque |
| `ultimos_pagamentos` | array | Últimos pagamentos |
| `atividades_recentes` | array | Feed de atividades |

---

### `usuario`

```json
{
  "id": 1,
  "nome": "Victor Muller",
  "email": "victor@covertix.net.br",
  "tipo": "ADMIN",
  "nome_empresa": "Covertix",
  "foto": "/uploads/..."
}
```

- `tipo`: `ADMIN` | `CLIENTE`
- `nome_empresa` / `foto`: podem ser `null`

---

### `kpis`

Métricas prontas para cards da home.

| Campo | Tipo | Notas |
|---|---|---|
| `total_sites` | int | |
| `sites_ativos` | int | status `ATIVO` |
| `sites_inativos` | int | status `INATIVO` |
| `sites_em_desenvolvimento` | int | status `EM_DESENVOLVIMENTO` |
| `total_biolinks` | int | registros em biolinks |
| `sites_biolink` | int | sites com `tipo = BIOLINK` |
| `total_clientes` | int | ADMIN: todos; CLIENTE: 1 |
| `total_usuarios` | int | |
| `usuarios_ativos` | int | `ativo = true` |
| `assinaturas_ativas` | int | `ACTIVE` |
| `assinaturas_inativas` | int | `INACTIVE` |
| `assinaturas_expiradas` | int | `EXPIRED` |
| `total_pago` | decimal | soma `RECEIVED` + `CONFIRMED` |
| `total_pendente` | decimal | soma `PENDING` + `OVERDUE` |
| `quantidade_pagamentos` | int | total de pagamentos |
| `quantidade_pendentes` | int | status `PENDING` |
| `quantidade_vencidos` | int | status `OVERDUE` |
| `ticket_medio_pago` | decimal | `total_pago / qtd pagos` (0 se não houver) |
| `mrr_estimado` | decimal | assinaturas `ACTIVE` normalizadas para mensal |
| `receita_mes_atual` | decimal | pagos no mês corrente |
| `receita_mes_anterior` | decimal | pagos no mês anterior |
| `variacao_receita_percentual` | decimal | % vs mês anterior |

**Sugestão de UI**

- Cards superiores: sites, biolinks, assinaturas ativas, receita do mês
- Badge de variação: verde se `variacao_receita_percentual > 0`, vermelho se `< 0`
- `mrr_estimado` e `ticket_medio_pago` em cards financeiros

---

### `distribuicoes`

Listas sempre com todas as chaves possíveis (quantidade/valor `0` quando não houver dados).

#### `sites_por_status`

```json
{ "chave": "ATIVO", "label": "Ativo", "quantidade": 3 }
```

Chaves: `ATIVO`, `INATIVO`, `EM_DESENVOLVIMENTO`

#### `sites_por_tipo`

Chaves: `BIOLINK`, `LANDING_PAGE`, `SITE_COMERCIAL`  
(`label` amigável incluso)

#### `pagamentos_por_status`

```json
{ "chave": "PENDING", "quantidade": 3, "valor": 320.00 }
```

Chaves: `PENDING`, `RECEIVED`, `CONFIRMED`, `OVERDUE`, `CANCELLED`, `REFUNDED`, `FAILED`

#### `pagamentos_por_forma`

Chaves: `PIX`, `CREDIT_CARD`, `BOLETO`

#### `assinaturas_por_status`

Chaves: `ACTIVE`, `INACTIVE`, `EXPIRED`  
(sem `label`)

#### `assinaturas_por_ciclo`

Chaves: `WEEKLY`, `BIWEEKLY`, `MONTHLY`, `BIMONTHLY`, `QUARTERLY`, `SEMIANNUALLY`, `YEARLY`  
(sem `label`)

**Sugestão de UI:** pizza/donut para status; barras para forma de pagamento.

---

### `series`

Todas as séries cobrem o mesmo intervalo de meses (`periodo_meses`), **incluindo meses zerados**.

Cada ponto tem:

```json
{ "ano": 2025, "mes": 9, "label": "2025-09", "...": "..." }
```

Use `label` no eixo X dos gráficos.

#### `receita_mensal`

```json
{
  "ano": 2025,
  "mes": 9,
  "label": "2025-09",
  "valor_pago": 200.00,
  "valor_pendente": 50.00,
  "quantidade_pagos": 2,
  "quantidade_pendentes": 1
}
```

#### `novos_clientes_mensal` / `novos_sites_mensal` / `novas_assinaturas_mensal`

```json
{ "ano": 2025, "mes": 9, "label": "2025-09", "quantidade": 1 }
```

**Sugestão de UI:** gráfico de área/linha com `valor_pago`; série secundária opcional com `valor_pendente`.

---

### `funil`

```json
{
  "clientes": 3,
  "clientes_com_site": 3,
  "clientes_com_assinatura": 2,
  "clientes_com_pagamento_pago": 2,
  "taxas": {
    "cliente_para_site": 100.0,
    "site_para_assinatura": 66.67,
    "assinatura_para_pago": 100.0,
    "cliente_para_pago": 66.67
  }
}
```

Taxas já vêm em **porcentagem** (0–100), com 2 casas.

---

### `alertas`

Ordenados por severidade: `CRITICAL` → `WARNING` → `INFO`.

```json
{
  "id": "pagamento_overdue_88",
  "tipo": "PAGAMENTO_VENCIDO",
  "severidade": "CRITICAL",
  "titulo": "Pagamento vencido",
  "mensagem": "Fatura #88 está OVERDUE.",
  "entidade": "PAGAMENTO",
  "entidade_id": 88,
  "cliente_id": 4,
  "cliente_nome": "Empresa X",
  "data_referencia": "2026-08-01T00:00:00",
  "valor": 99.90
}
```

| `tipo` | `severidade` | Quando aparece |
|---|---|---|
| `PAGAMENTO_VENCIDO` | `CRITICAL` | Pagamento `OVERDUE` |
| `ASSINATURA_VENCENDO` | `WARNING` | Assinatura `ACTIVE` com cobrança nos próximos 7 dias |
| `SITE_INATIVO` | `INFO` | Site com status `INATIVO` |

| `entidade` | Navegação sugerida |
|---|---|
| `PAGAMENTO` | detalhe/lista de pagamentos (`entidade_id`) |
| `ASSINATURA` | detalhe da assinatura |
| `SITE` | detalhe do site |

`data_referencia` e `valor` podem ser `null` (ex.: alerta de site inativo).

---

### `tops`

#### `clientes_por_receita`

```json
{
  "cliente_id": 4,
  "cliente_nome": "Empresa X",
  "total_pago": 800.00,
  "quantidade_pagamentos": 5
}
```

#### `sites_recentes`

```json
{
  "id": 10,
  "nome": "Meu Site",
  "tipo": "BIOLINK",
  "status": "ATIVO",
  "cliente_id": 4,
  "cliente_nome": "Empresa X",
  "created_at": "2026-08-01T12:00:00"
}
```

---

### `assinatura_destaque`

Sempre presente.

```json
{
  "ativa": true,
  "assinatura_id": 12,
  "descricao": "Plano Mensal",
  "status": "ACTIVE",
  "valor": 99.90,
  "ciclo": "MONTHLY",
  "metodo_pagamento": "PIX",
  "proxima_cobranca": "2026-09-01T00:00:00",
  "cliente_id": 4,
  "cliente_nome": "Empresa X"
}
```

Se não houver assinatura `ACTIVE`:

```json
{ "ativa": false }
```

(demais campos `null` / omitíveis)

No perfil **CLIENTE**, use este bloco no card “Minha assinatura”.  
No **ADMIN**, é a assinatura ACTIVE mais recente global.

---

### `ultimos_pagamentos`

```json
{
  "id": 88,
  "valor": 99.90,
  "descricao": "Mensalidade",
  "status": "PENDING",
  "forma_pagamento": "PIX",
  "parcelas": 1,
  "asaas_payment_id": "pay_xxx",
  "invoice_url": "https://...",
  "comprovante_url": null,
  "created_at": "2026-08-08T10:00:00",
  "data_confirmacao": null,
  "cliente_id": 4,
  "cliente_nome": "Empresa X"
}
```

Quantidade: no mínimo 10 (ou `limite_tops` se maior).

Status úteis para badge de cor:

| Status | Sugestão visual |
|---|---|
| `RECEIVED` / `CONFIRMED` | sucesso |
| `PENDING` | alerta |
| `OVERDUE` | erro |
| `CANCELLED` / `FAILED` / `REFUNDED` | neutro/erro |

---

### `atividades_recentes`

Feed já ordenado do mais recente para o mais antigo.

```json
{
  "id": "pagamento_88_created",
  "tipo": "PAGAMENTO_CRIADO",
  "titulo": "Novo pagamento",
  "descricao": "Fatura de R$ 99,90 criada",
  "entidade": "PAGAMENTO",
  "entidade_id": 88,
  "cliente_id": 4,
  "cliente_nome": "Empresa X",
  "created_at": "2026-08-08T10:00:00"
}
```

Tipos possíveis hoje:

| `tipo` | `entidade` |
|---|---|
| `PAGAMENTO_CRIADO` | `PAGAMENTO` |
| `SITE_CRIADO` | `SITE` |
| `ASSINATURA_CRIADA` | `ASSINATURA` |
| `CLIENTE_CRIADO` | `CLIENTE` (somente ADMIN) |

---

## Exemplo completo (ilustrativo)

```json
{
  "gerado_em": "2026-08-09T20:00:00Z",
  "escopo": "ADMIN",
  "periodo_meses": 12,
  "usuario": {
    "id": 1,
    "nome": "Victor Muller",
    "email": "victor@covertix.net.br",
    "tipo": "ADMIN",
    "nome_empresa": null,
    "foto": null
  },
  "kpis": {
    "total_sites": 4,
    "sites_ativos": 3,
    "sites_inativos": 0,
    "sites_em_desenvolvimento": 1,
    "total_biolinks": 4,
    "sites_biolink": 4,
    "total_clientes": 3,
    "total_usuarios": 5,
    "usuarios_ativos": 4,
    "assinaturas_ativas": 2,
    "assinaturas_inativas": 1,
    "assinaturas_expiradas": 0,
    "total_pago": 1500.50,
    "total_pendente": 320.00,
    "quantidade_pagamentos": 18,
    "quantidade_pendentes": 3,
    "quantidade_vencidos": 1,
    "ticket_medio_pago": 250.08,
    "mrr_estimado": 199.90,
    "receita_mes_atual": 450.00,
    "receita_mes_anterior": 390.00,
    "variacao_receita_percentual": 15.38
  },
  "distribuicoes": { "...": "..." },
  "series": { "...": "..." },
  "funil": { "...": "..." },
  "alertas": [],
  "tops": {
    "clientes_por_receita": [],
    "sites_recentes": []
  },
  "assinatura_destaque": { "ativa": false },
  "ultimos_pagamentos": [],
  "atividades_recentes": []
}
```

---

## Como migrar a tela Início

### Antes

Vários GETs em paralelo, ex.:

1. listar sites → contar
2. listar biolinks → contar
3. listar clientes → contar
4. listar usuários → contar
5. `GET /api/v1/financeiro/dashboard`
6. `GET /api/v1/pagamentos/ultimos`

### Depois

```
1 request → GET /api/v1/dashboard/inicio
```

Sugestão de binding:

| Widget / seção | Campo |
|---|---|
| Header / avatar | `usuario` |
| Cards KPI | `kpis` |
| Gráficos de distribuição | `distribuicoes` |
| Gráfico temporal | `series.receita_mensal` |
| Funil | `funil` |
| Lista de alertas | `alertas` |
| Ranking clientes | `tops.clientes_por_receita` |
| Sites recentes | `tops.sites_recentes` |
| Card assinatura | `assinatura_destaque` |
| Lista pagamentos | `ultimos_pagamentos` |
| Timeline | `atividades_recentes` |

Os endpoints antigos (`/financeiro/dashboard`, `/pagamentos/ultimos`, listagens) **continuam existindo** — só não precisam mais ser chamados na Home.

---

## Erros comuns

| HTTP | Situação |
|---|---|
| `401` | Sem token / token inválido |
| `403` | Usuário CLIENTE sem empresa vinculada |
| `500` | Erro interno |

Corpo de erro segue o padrão global da API (`timestamp`, `status`, `error`, `message`).

---

## Performance / boas práticas no front

1. Chamar **uma vez** ao abrir a Home (e no pull-to-refresh).
2. Não refetch a cada navegação de tab se o estado ainda estiver fresco (cache local de 30–60s é suficiente).
3. Não paginar este endpoint — ele já devolve só resumos e top N.
4. Para detalhes, navegar para as rotas específicas usando `entidade_id` / `cliente_id`.
5. Tratar listas vazias com empty state (não assumir que sempre há alertas/pagamentos).

---

## Relação com endpoints financeiros legados

| Legado | Equivalente neste dashboard |
|---|---|
| `GET /api/v1/financeiro/dashboard` | `kpis` + `assinatura_destaque` |
| `GET /api/v1/pagamentos/ultimos` | `ultimos_pagamentos` |

Para a Home, prefira sempre `/api/v1/dashboard/inicio`.
