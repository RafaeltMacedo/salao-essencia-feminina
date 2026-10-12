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
- `database.rules.json` é uma **cópia** versionada; a fonte da verdade é o
  Firebase Console. Antes de propor mudança, conferir a versão no ar (via
  Firebase MCP) e avisar se as duas divergirem.
- Ninguém pode se autopromover a `tipo: "cabeleireira"`. Toda mudança de regra
  em `usuarios/$uid` deve manter essa garantia.
- `firebase.json`: nunca remover `.well-known` do que é publicado (quebra o
  App Link) e nunca tirar do `ignore` os arquivos internos (`*.md`,
  `database.rules.json`, `.claude/**`) — `"public": "."` publica a pasta inteira.
- Cloud Functions: manter `maxInstances` (proteção de custo no plano Blaze) e
  os imports modulares do `firebase-admin` 14+.
