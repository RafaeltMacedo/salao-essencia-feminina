---
paths:
  - "cabeleleiro/app/src/main/java/**/BancoController.java"
  - "cabeleleiro/app/src/main/java/**/AgendarActivity.java"
  - "cabeleleiro/app/src/main/java/**/ConfiguracaoController.java"
  - "site-agendamento/index.html"
---

# Lógica de reserva duplicada (app Java ↔ site JS)

A regra de "horário livre" existe **duas vezes**, uma em cada linguagem. Se
só um lado mudar, app e site passam a discordar sobre quais horários estão
livres — e um pode reservar por cima do outro.

| Conceito | App Android (Java) | Site (`index.html`) |
|---|---|---|
| Bloco de 20 min | `BancoController.GRANULARIDADE_MINUTOS` | `const GRANULARIDADE_MINUTOS` |
| Data como chave (`/` → `-`) | `BancoController.chaveData()` | `function chaveData()` |
| Nº de blocos (arredonda pra cima) | `BancoController` (`Math.ceil`) | `Math.ceil(duracaoMinutos / ...)` |
| Horário de abertura/fechamento | `AgendarActivity.HORA_ABERTURA` / `HORA_FECHAMENTO` | `const HORA_ABERTURA` / `HORA_FECHAMENTO` |
| Reserva atômica multi-path | `BancoController` (`updateChildren`) | `db.ref().update(atualizacoes)` |
| Dias de funcionamento | `ConfiguracaoController` | leitura de `configuracoes/diasFuncionamento` |

## Ao alterar qualquer item acima

1. Aplicar a mesma mudança no outro lado, na mesma tarefa.
2. Conferir o callback de `snap.forEach()` nos dois lados (nunca retornar valor truthy).
3. Se mudar o formato de algo gravado no banco, atualizar o schema em
   `cabeleleiro/CLAUDE.md` e checar `site-agendamento/database.rules.json`.
4. Avisar o usuário que o app precisa de APK novo **e** o site de deploy novo.
