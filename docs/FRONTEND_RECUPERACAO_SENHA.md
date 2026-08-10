# Recuperação de senha — guia para o front

Documentação das rotas públicas de recuperação de conta/senha para montar as telas no app.

Base: `/api/v1/auth`  
Autenticação: **não** envie JWT nessas rotas.  
Formato JSON: **snake_case**.

Rate limit (mesmo bucket do login): **5 requisições/minuto por IP**. Em excesso → `429`.

---

## Fluxo sugerido de telas

```
[Login] → "Esqueci minha senha"
    → Tela 1: informar e-mail
        → POST /recuperar-senha
    → Tela 2: informar código de 6 dígitos
        → POST /verificar-codigo
    → Tela 3: informar nova senha
        → POST /redefinir-senha
    → Voltar ao login
```

1. Na tela de login, link **Esqueci minha senha**.
2. Usuário informa o e-mail cadastrado.
3. Backend envia o código por e-mail e devolve o `usuario_id`.
4. Guarde `usuario_id` (e opcionalmente `expira_em`) no estado da navegação / storage temporário.
5. Usuário digita o código de 6 números.
6. Front chama a verificação com `usuario_id` + `codigo`.
7. Se `valido = true`, avance para a tela de nova senha (mantenha `usuario_id` e `codigo` no estado).
8. Usuário informa a nova senha e o front chama `/redefinir-senha` com `usuario_id` + `codigo` + `nova_senha`.
9. Em sucesso, limpe o estado temporário e redirecione para o login.

O código **nunca** vem na resposta da API — só no e-mail.  
A redefinição **não** pede a senha antiga; o código validado autoriza a troca.

---

## 1) Solicitar código

### Request

```
POST /api/v1/auth/recuperar-senha
Content-Type: application/json
```

```json
{
  "email": "usuario@email.com"
}
```

| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `email` | string | sim | e-mail válido, máx. 255 |

### Response `200`

```json
{
  "usuario_id": 12,
  "mensagem": "Código de recuperação enviado para o e-mail cadastrado.",
  "enviado_em": "2026-08-09T20:30:00",
  "expira_em": "2026-08-09T22:30:00"
}
```

| Campo | Uso no front |
|---|---|
| `usuario_id` | Guardar e enviar na verificação e na redefinição |
| `mensagem` | Feedback na UI |
| `enviado_em` | Opcional (auditoria/debug) |
| `expira_em` | Opcional: countdown / aviso de validade |

### Erros comuns

| HTTP | Situação | Exemplo de `message` |
|---|---|---|
| `400` | Validação do e-mail | `"Erro de validação"` + mapa `errors` |
| `400` | E-mail inexistente / usuário inativo | `"Não foi possível processar a solicitação de recuperação de senha."` |
| `400` | Falha no envio do e-mail | `"Não foi possível enviar o e-mail de recuperação de senha"` |
| `429` | Rate limit | `"Limite de requisições excedido. Tente novamente em breve."` |

### UX recomendada — Tela 1

- Campo: e-mail
- Botão: **Enviar código**
- Em sucesso: ir para a tela do código e informar que o e-mail foi enviado
- Em erro genérico de negócio: mostrar a `message` da API (não inventar “usuário não existe”)
- Permitir **reenviar código** (nova chamada em `/recuperar-senha`); o último código enviado é o único válido

---

## 2) Verificar código

### Request

```
POST /api/v1/auth/verificar-codigo
Content-Type: application/json
```

```json
{
  "usuario_id": 12,
  "codigo": "483920"
}
```

| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `usuario_id` | number | sim | ID retornado em `/recuperar-senha` |
| `codigo` | string | sim | exatamente **6 dígitos** (`0-9`) |

### Response `200`

```json
{
  "usuario_id": 12,
  "valido": true,
  "mensagem": "Código verificado com sucesso."
}
```

### Erros comuns

| HTTP | Situação | Exemplo de `message` |
|---|---|---|
| `400` | Código errado, expirado, antigo ou usuário inválido | `"Código inválido ou expirado."` |
| `400` | Validação (tamanho/formato) | `"Erro de validação"` + mapa `errors` |
| `429` | Rate limit | `"Limite de requisições excedido. Tente novamente em breve."` |

### Regras importantes

- Só vale o **último** código enviado para aquele `usuario_id`
- Se o usuário pediu um novo código, o anterior deixa de funcionar
- Validade: **2 horas** a partir de `enviado_em`
- Código antigo / expirado / incorreto → mesma mensagem genérica

### UX recomendada — Tela 2

- Input numérico de 6 dígitos (máscara / OTP)
- Botão: **Verificar código**
- Link: **Reenviar código** → volta a chamar `/recuperar-senha` com o mesmo e-mail e atualiza o `usuario_id`/`expira_em` salvos
- Se `valido === true`, avance para a tela de nova senha mantendo `usuario_id` e `codigo`
- Em erro, limpar o campo do código e mostrar a `message`

---

## 3) Redefinir senha

### Request

```
POST /api/v1/auth/redefinir-senha
Content-Type: application/json
```

```json
{
  "usuario_id": 12,
  "codigo": "483920",
  "nova_senha": "novaSenhaSegura"
}
```

| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `usuario_id` | number | sim | ID retornado em `/recuperar-senha` |
| `codigo` | string | sim | exatamente **6 dígitos** (`0-9`), o mesmo já verificado |
| `nova_senha` | string | sim | entre **8** e **128** caracteres |

### Response `200`

```json
{
  "usuario_id": 12,
  "mensagem": "Senha alterada com sucesso."
}
```

### Erros comuns

| HTTP | Situação | Exemplo de `message` |
|---|---|---|
| `400` | Código errado, expirado, antigo ou usuário inválido | `"Código inválido ou expirado."` |
| `400` | Validação (tamanho/formato) | `"Erro de validação"` + mapa `errors` |
| `429` | Rate limit | `"Limite de requisições excedido. Tente novamente em breve."` |

### Regras importantes

- O backend **revalida** o código neste passo (não basta ter passado em `/verificar-codigo`)
- Não exige a senha antiga
- Após sucesso, os códigos de recuperação daquele usuário são invalidados (não dá para reutilizar)
- Após sucesso, o backend envia um e-mail de aviso ao endereço da conta informando que a senha foi alterada
- Se o código expirou entre a verificação e a redefinição, peça um novo em `/recuperar-senha`

### UX recomendada — Tela 3

- Campos: nova senha + confirmação (confirmação só no front)
- Botão: **Alterar senha**
- Enviar `usuario_id`, `codigo` e `nova_senha`
- Em sucesso: feedback, limpar estado temporário e redirecionar para o login
- Em erro de código: voltar para a tela do código ou oferecer reenvio

---

## Formato de erro da API

```json
{
  "timestamp": "2026-08-09T20:35:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Código inválido ou expirado.",
  "errors": null
}
```

Em validação Bean Validation, `errors` vem como mapa campo → mensagem, por exemplo:

```json
{
  "status": 400,
  "message": "Erro de validação",
  "errors": {
    "codigo": "O código deve ter exatamente 6 dígitos"
  }
}
```

---

## Checklist rápido para o front

- [ ] Tela de e-mail chama `POST /api/v1/auth/recuperar-senha`
- [ ] Persiste `usuario_id` (e preferencialmente o e-mail) entre as telas
- [ ] Tela de código chama `POST /api/v1/auth/verificar-codigo`
- [ ] Após código válido, persiste também o `codigo` para a próxima tela
- [ ] Tela de nova senha chama `POST /api/v1/auth/redefinir-senha`
- [ ] Input do código aceita só 6 números
- [ ] Nova senha com mínimo de 8 caracteres
- [ ] Trata `400` e `429` com a `message` da API
- [ ] Reenvio de código usa de novo `/recuperar-senha`
- [ ] Não espera o código na resposta da API
- [ ] Não exige token JWT nessas rotas
- [ ] Após sucesso em `/redefinir-senha`, limpa o estado e volta ao login
