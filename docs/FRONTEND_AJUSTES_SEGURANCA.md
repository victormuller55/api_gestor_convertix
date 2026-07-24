# Ajustes necessários no Frontend — Segurança da API

Este documento lista o que o frontend precisa adaptar após o hardening da API Gestor.

---

## 1. Origem (CORS) — obrigatório

A API **não aceita mais qualquer origem**.

| Ambiente | Origens permitidas |
|----------|-------------------|
| Produção | `https://gestor.convertix.net.br` |
| Local | `http://localhost:3000`, `http://localhost:5173`, `http://localhost:8080` |

**O que fazer:**
- Em produção, o front **deve** ser servido em `https://gestor.convertix.net.br`.
- Em local, rode o front em uma das portas acima (3000 / 5173 / 8080).
- Se usar outra porta/domínio, a API bloqueia a requisição no navegador (erro CORS).

---

## 2. Rate limiting — tratar HTTP 429

Limites por IP:

| Endpoint | Limite |
|----------|--------|
| `POST /api/v1/auth/login` | 5 req/min |
| `POST /api/v1/usuarios/novo` e `POST /api/v1/clientes/novo` | 10 req/min |
| Demais rotas `/api/**` | 100 req/min |

**Resposta 429 (exemplo):**
```json
{
  "timestamp": "...",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Limite de requisições excedido. Tente novamente em breve."
}
```

**Headers úteis:**
- `Retry-After` — segundos para tentar de novo
- `X-RateLimit-Limit`
- `X-RateLimit-Remaining`

**O que fazer no front:**
- Interceptor Axios/Fetch: se `status === 429`, mostrar mensagem amigável e respeitar `Retry-After`.
- No login, evitar loops de retry automático.
- Desabilitar botão de submit por alguns segundos após 429.

---

## 3. Login — mensagem genérica

A API sempre responde com:

> **Usuário ou senha inválidos.**

Isso inclui e-mail inexistente, senha errada e usuário inativo.

**O que fazer:**
- Não esperar mais mensagens como `"Usuário inativo"` ou `"Email ou senha inválidos"` (texto antigo).
- Exibir a mensagem retornada em `message` do `ErrorResponse`.

---

## 4. Senha — mínimo 8 caracteres

Cadastro/atualização de usuário e cliente:

- Senha **obrigatória** na criação: mínimo **8**, máximo **128** caracteres.
- Na atualização: se enviar `senha`, também deve ter entre 8 e 128; para não alterar, **não envie** o campo (ou envie `null`).

**O que fazer:**
- Validar no formulário (`minLength={8}`) antes de enviar.
- Atualizar mensagens de erro de validação do formulário.

---

## 5. Validações de campos (evitar 400)

A API rejeita payloads inválidos com `400` e `errors` por campo.

Pontos que mais impactam o front:

| Campo | Regra |
|-------|-------|
| BioLink `url` | Deve começar com `http://` ou `https://` |
| BioLink `nome_usuario` | 3–50 chars; só `a-zA-Z0-9._-` |
| Landing page `slug` | Só letras, números e hífen |
| Cartão `number` | 13–19 dígitos |
| Cartão `expiry_month` | `01`–`12` |
| Cartão `ccv` | 3 ou 4 dígitos |
| Documento cliente | 11–18 chars (pode formatado); validação final no backend |
| CEP titular | `12345678` ou `12345-678` |

**O que fazer:**
- Alinhar máscaras/validações do formulário com essas regras.
- Exibir `errors` do response (mapa campo → mensagem).

---

## 6. Upload de imagens

Continua aceitando apenas JPEG, PNG e WebP (máx. 5 MB).

**Novo:** o conteúdo real do arquivo precisa bater com o tipo (magic bytes). Renomear um `.exe` para `.png` passa a falhar.

**O que fazer:**
- Manter `accept="image/jpeg,image/png,image/webp"`.
- Tratar mensagem: `"Conteúdo do arquivo não corresponde ao tipo informado"`.

---

## 7. Swagger em produção

Swagger **não existe** em produção (`404` / acesso negado).

**O que fazer:**
- Não linkar `/swagger-ui.html` no front de produção.
- Documentação da API: usar ambiente local ou este repositório.

---

## 8. Headers de resposta / cache

Endpoints `/api/**` enviam:

```
Cache-Control: no-store, no-cache, must-revalidate, max-age=0
Pragma: no-cache
Expires: 0
```

**O que fazer:**
- Não depender de cache HTTP de respostas da API no browser.
- Token JWT continua só em memória/`localStorage`/`sessionStorage` (a API não usa cookies).

---

## 9. Autenticação JWT

- Continuar enviando: `Authorization: Bearer <token>`
- Token expira em **24 horas** (antes: até meia-noite do dia seguinte).
- Se o usuário for desativado, o token deixa de autenticar mesmo antes de expirar → front deve redirecionar para login em **401**.

**O que fazer:**
- Interceptor: em `401`, limpar token e redirecionar para `/login`.
- Opcional: renovar sessão perto do fim das 24h (ainda não há refresh token).

---

## 10. Erros padronizados

Formato estável:

```json
{
  "timestamp": "2026-07-23T22:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Erro de validação",
  "errors": {
    "senha": "A senha deve ter entre 8 e 128 caracteres"
  }
}
```

**O que fazer:**
- Centralizar leitura de `message` e `errors`.
- Não depender de stacktrace ou textos internos (não são mais expostos).

---

## 11. Webhook Asaas (não é front web, mas integração)

Se o front/ops configurar webhooks, o header/token do Asaas **precisa** bater com `ASAAS_WEBHOOK_TOKEN`. Sem token, o webhook é rejeitado.

---

## Checklist rápido para o time de front

- [ ] Front de produção em `https://gestor.convertix.net.br`
- [ ] Dev local em porta 3000, 5173 ou 8080
- [ ] Tratar HTTP **429** com `Retry-After`
- [ ] Login: mensagem genérica única
- [ ] Senha mínima 8 caracteres
- [ ] URLs de BioLink com `http://` ou `https://`
- [ ] Tratar 401 → logout
- [ ] Upload só JPEG/PNG/WebP reais
- [ ] Remover links de Swagger em produção
