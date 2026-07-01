# Ícone dos itens do BioLink — enum `BioLinkItemIcone`

Documento para o frontend aplicar a mudança no cadastro e exibição dos links da bio.

---

## Resumo

| Antes | Depois |
|-------|--------|
| Campo `icone` como **string livre** (emoji enviado pelo front) | Campo `icone` como **enum** com valores fixos |
| Ex.: `"icone": "📸"` | Ex.: `"icone": "INSTAGRAM"` |

O backend passa a validar o ícone. Valores fora da lista retornam erro **400** na desserialização do JSON.

Itens já salvos com emoji ou texto inválido têm o ícone definido como `null` na migração automática. O usuário precisa escolher novamente o ícone na edição.

---

## Convenção JSON

- Propriedades continuam em **snake_case** (`biolink_id`, `created_at`, etc.).
- O valor de `icone` é o **nome do enum em maiúsculas**, igual aos demais enums da API (`TipoSite`, `StatusSite`, etc.).

---

## Valores aceitos

| Valor enviado | Plataforma / uso |
|---------------|------------------|
| `WHATSAPP` | WhatsApp |
| `INSTAGRAM` | Instagram |
| `TIKTOK` | TikTok |
| `YOUTUBE` | YouTube |
| `FACEBOOK` | Facebook |
| `LINKEDIN` | LinkedIn |
| `X` | X (Twitter) |
| `TELEGRAM` | Telegram |
| `DISCORD` | Discord |
| `SPOTIFY` | Spotify |
| `PINTEREST` | Pinterest |
| `THREADS` | Threads |
| `SNAPCHAT` | Snapchat |
| `TWITCH` | Twitch |
| `GITHUB` | GitHub |
| `BEHANCE` | Behance |
| `DRIBBBLE` | Dribbble |
| `MEDIUM` | Medium |
| `SUBSTACK` | Substack |
| `GOOGLE_MAPS` | Google Maps |
| `OUTROS` | Outros / link genérico |

O campo continua **opcional** (`null` = sem ícone).

---

## Endpoints afetados

| Método | Endpoint | Campo |
|--------|----------|-------|
| POST | `/api/v1/biolinks/itens/novo` | Request: `icone` |
| PUT | `/api/v1/biolinks/itens/alterar-dados?biolink_id={id}&id={id}` | Request: `icone` |
| GET | `/api/v1/biolinks/itens?biolink_id={id}` | Response: `icone` |
| GET | `/api/v1/biolinks/publico?site_id={id}` | Response dos itens: `icone` |

---

## Exemplo — cadastrar item

```json
{
  "biolink_id": 1,
  "titulo": "Instagram",
  "url": "https://instagram.com/convertix",
  "icone": "INSTAGRAM",
  "ordem": 1,
  "ativo": true
}
```

## Exemplo — response

```json
{
  "id": 10,
  "biolink_id": 1,
  "titulo": "Instagram",
  "url": "https://instagram.com/convertix",
  "icone": "INSTAGRAM",
  "ordem": 1,
  "ativo": true,
  "created_at": "2026-07-01T10:00:00",
  "updated_at": "2026-07-01T10:00:00"
}
```

## Exemplo — endpoint público (item)

```json
{
  "titulo": "WhatsApp",
  "url": "https://wa.me/5541999999999",
  "icone": "WHATSAPP",
  "ordem": 0
}
```

---

## O que fazer no frontend

- [ ] Parar de enviar emoji no campo `icone`
- [ ] Enviar um dos valores do enum (tabela acima)
- [ ] Mapear cada valor para o ícone visual correspondente no app (SVG, font icon, etc.)
- [ ] Oferecer seletor de plataforma com as 21 opções + opção “sem ícone”
- [ ] Tratar `icone: null` na exibição pública e na edição de itens migrados
- [ ] Atualizar models/DTOs: `icone` de `String` para enum tipado

### Sugestão de enum Dart

```dart
enum BioLinkItemIcone {
  whatsapp,
  instagram,
  tiktok,
  youtube,
  facebook,
  linkedin,
  x,
  telegram,
  discord,
  spotify,
  pinterest,
  threads,
  snapchat,
  twitch,
  github,
  behance,
  dribbble,
  medium,
  substack,
  googleMaps,
  outros;

  String toJson() => name == 'googleMaps'
      ? 'GOOGLE_MAPS'
      : name == 'x'
          ? 'X'
          : name.toUpperCase();
}
```

(Ajuste o `fromJson` conforme o padrão do projeto.)

---

## Erros

| Situação | HTTP | Comportamento |
|----------|------|---------------|
| Valor inválido em `icone` | 400 | JSON não é aceito (ex.: `"icone": "📸"` ou `"instagram"`) |
| Ícone omitido | — | Salvo como `null` |

Use sempre o nome do enum **exatamente** como na tabela (`INSTAGRAM`, não `instagram`).

---

*Backend Gestor — julho/2026.*
