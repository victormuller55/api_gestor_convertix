# Paginação nas listagens — guia para o Frontend

As rotas de listagem abaixo **deixaram de retornar um array puro** e passam a retornar um objeto paginado.

## Rotas afetadas

| Recurso | Método | Endpoint |
|---------|--------|----------|
| Usuários | `GET` | `/api/v1/usuarios` |
| Clientes | `GET` | `/api/v1/clientes` |
| Sites | `GET` | `/api/v1/sites` |
| Biolinks | `GET` | `/api/v1/biolinks` |
| Pagamentos / Faturas (histórico) | `GET` | `/api/v1/pagamentos/historico` |
| Assinaturas | `GET` | `/api/v1/assinaturas` |

Os filtros já existentes (`id`, `query`, `ativo`, `status`, etc.) **continuam iguais**. Só entram os parâmetros de página.

---

## Query params de paginação

| Param | Tipo | Padrão | Descrição |
|-------|------|--------|-----------|
| `page` | `number` | `0` | Número da página (**base 0**) |
| `size` | `number` | `30` | Itens por página (máximo `100`) |

Exemplos:

```http
GET /api/v1/clientes?page=0&size=30
GET /api/v1/clientes?query=acme&page=1&size=30
GET /api/v1/pagamentos/historico?page=0
GET /api/v1/assinaturas?status=ACTIVE&page=2&size=30
```

---

## Formato da resposta

Antes:

```json
[ { "...": "item" }, { "...": "item" } ]
```

Agora (snake_case):

```json
{
  "content": [
    { "...": "item" }
  ],
  "page": 0,
  "size": 30,
  "total_elements": 120,
  "total_pages": 4
}
```

| Campo | Significado |
|-------|-------------|
| `content` | Lista de itens da página atual |
| `total_elements` | Número total de itens (todas as páginas) |
| `size` | Número máximo de itens por página (pedido/normalizado) |
| `page` | Número da página atual (base 0) |
| `total_pages` | Número máximo de páginas |

---

## Datas (`created_at`, `updated_at`, etc.)

As datas **não vêm mais como array** (`[2026, 7, 1, ...]`). Vêm como **string ISO-8601**:

```json
{
  "created_at": "2026-07-01T19:29:25.539960",
  "updated_at": "2026-07-23T19:28:30.524014"
}
```

| Tipo no backend | Exemplo JSON |
|-----------------|--------------|
| `LocalDateTime` | `"2026-07-01T19:29:25.539960"` |
| `LocalDate` | `"2026-07-01"` |

### O que fazer no front

1. Tipar datas como `string` (não `number[]`).
2. Para exibir, usar `new Date(valor)` ou uma lib (`dayjs` / `date-fns` / `luxon`).
3. Remover qualquer parser que montava data a partir do array antigo.

```ts
// ❌ antigo (array)
created_at: number[];

// ✅ novo (ISO string)
created_at: string;
updated_at: string;

function formatarData(iso: string) {
  return new Date(iso).toLocaleString("pt-BR");
}
```

---

## O que mudar no front (paginação)

1. **Parar de tratar a resposta como array.** Usar `response.content` (ou `response.data.content`, conforme o client HTTP).
2. **Enviar `page` e `size`** nas listagens (padrão recomendado: `size=30`).
3. **Controles de UI:**
   - total de itens → `total_elements`
   - página atual → `page` (somar `+1` só na exibição, se quiser base 1)
   - total de páginas → `total_pages`
   - itens por página → `size`
4. **Próxima / anterior:**
   - Anterior: `page > 0` → `page - 1`
   - Próxima: `page + 1 < total_pages` → `page + 1`
5. **Filtros:** ao mudar `query` / status / etc., resetar para `page=0`.
6. **Datas:** tipar/parsear como string ISO (ver seção acima).

### Exemplo TypeScript

```ts
type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  total_elements: number;
  total_pages: number;
};

type Cliente = {
  id: number;
  nome_empresa: string;
  documento: string;
  email: string;
  telefone: string;
  foto: string | null;
  created_at: string; // "2026-07-01T19:29:25.539960"
  updated_at: string;
};

async function listarClientes(page = 0, size = 30, query?: string) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  if (query) params.set("query", query);

  const res = await fetch(`/api/v1/clientes?${params}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return (await res.json()) as PageResponse<Cliente>;
}
```

---

## Breaking changes

1. Listagens: deixar de tratar a resposta como array; usar `content` + metadados de página.
2. Datas: deixar de tratar `created_at` / `updated_at` como `number[]`; passar a usar string ISO com `new Date(iso)` (ou lib equivalente).
