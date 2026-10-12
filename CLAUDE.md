# Essência Feminina — monorepo (app Android + site)

Sistema de agendamento de um salão de beleza. Dois projetos que compartilham
o **mesmo Firebase** (`cabeleleiro-ff23d`) — mesmo banco, mesmas regras,
mesma autenticação:

| Pasta | O que é | Detalhes |
|---|---|---|
| `cabeleleiro/` | App Android em **Java** (cliente + cabeleireira) | `cabeleleiro/CLAUDE.md` — schema do banco, regras, bugs conhecidos |
| `site-agendamento/` | Site (só cliente, `index.html` único) + Cloud Functions | `site-agendamento/CLAUDE.md` — deploy, functions, App Link |

**Leia o CLAUDE.md da pasta em que for mexer.** Os pontos "não corrija sem
entender" do `cabeleleiro/CLAUDE.md` valem para os dois projetos.

## Regras gerais

- Responder e comentar código em português.
- O usuário não é desenvolvedor profissional: explicar o porquê das mudanças
  em linguagem simples, sem jargão desnecessário.
- Mudança em regra de negócio compartilhada (reserva de horário, schema do
  banco) **sempre** nos dois projetos — ver `.claude/rules/`.
- Nunca fazer deploy (`firebase deploy`) nem alterar dados/regras do banco
  de produção sem confirmação explícita: é o app real do salão, com clientes
  reais.
- Antes de commitar, rodar `/code-review`. Antes de publicar versão,
  `/security-review`.

## Skills do projeto

- `/deploy-site` — publica o site com checagens (assetlinks, ignore).
- `/deploy-functions` — publica as Cloud Functions com os gotchas conhecidos.
- `/gerar-apk` — build e assinatura do APK.
