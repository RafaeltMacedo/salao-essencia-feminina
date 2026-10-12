---
paths:
  - "site-agendamento/database.rules.json"
  - "site-agendamento/firebase.json"
  - "site-agendamento/functions/**"
  - "site-agendamento/.well-known/**"
  - "cabeleleiro/app/src/main/java/**/UsuarioController.java"
  - "cabeleleiro/app/src/main/java/**/Cadastre_se.java"
---

# Firebase — produção real

- O banco `cabeleleiro-ff23d` é **produção**: clientes reais, telefones reais.
  Nunca gravar, apagar ou alterar regras sem confirmação explícita do usuário.
- `database.rules.json` é a fonte da verdade das regras (publicada com
  `firebase deploy --only database`, sempre com confirmação). Antes de
  publicar, conferir que a versão no ar não foi mudada pelo Console
  (`firebase database:get /.settings/rules --instance cabeleleiro-ff23d-default-rtdb`,
  pelo PowerShell) e testar no emulador; avisar se divergirem.
- Ninguém pode se autopromover a `tipo: "cabeleireira"`. Toda mudança de regra
  em `usuarios/$uid` deve manter essa garantia.
- `firebase.json`: nunca remover `.well-known` do que é publicado (quebra o
  App Link) e nunca tirar do `ignore` os arquivos internos (`*.md`,
  `database.rules.json`, `.claude/**`) — `"public": "."` publica a pasta inteira.
- Ferramentas do Firebase MCP que gravam em produção (`firebase_deploy`,
  `realtimedatabase_set_data`, `auth_update_user`, `messaging_send_message`)
  só com confirmação explícita, igual ao `firebase deploy` pelo terminal.
- Nunca ler chaves de assinatura (`*.jks`, `*.keystore`) nem `.env`, nem
  pelo terminal (`cat`, `Get-Content`).
- Cloud Functions: manter `maxInstances` (proteção de custo no plano Blaze) e
  os imports modulares do `firebase-admin` 14+.
