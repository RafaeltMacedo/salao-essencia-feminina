---
name: deploy-functions
description: Publica as Cloud Functions (aviso push de novo agendamento) do projeto site-agendamento/functions, tratando os erros conhecidos de deploy. Usar quando o usuário pedir para publicar ou atualizar as functions ou o aviso push.
disable-model-invocation: true
---

# Deploy das Cloud Functions

Função: `avisarNovoAgendamento` (dispara em `/agendamentos/{id}` criado).

## Antes

1. Testar se o código carrega:
   ```bash
   cd site-agendamento/functions
   npm install
   node -e "require('./index.js'); console.log('CARREGOU OK')"
   ```
2. Conferir que `maxInstances` continua definido (proteção de custo).
3. Conferir imports modulares: `firebase-admin/app`, `/database`, `/messaging`
   (nunca `admin.database()` — não existe no firebase-admin 14+).
4. **Pedir confirmação** ao usuário.

## Publicar

```bash
cd site-agendamento
firebase deploy --only functions
```

## Erros conhecidos (não depurar do zero)

| Erro | Solução |
|---|---|
| `Permission denied ... Eventarc Service Agent` | Propagação de permissão do Google. Esperar alguns minutos e repetir o deploy sem mudar nada. |
| `Cannot determine backend specification. Timeout after 10000` | `npm install --save firebase-functions@latest firebase-admin@latest` em `functions/`. |
| `admin.database is not a function` | Trocar para imports modulares (ver acima). |
| Erro pedindo plano Blaze | Projeto voltou para Spark — checar faturamento no Console. |

## Depois

Ver logs: `firebase functions:log --only avisarNovoAgendamento`
("Failed to retrieve log entries" costuma ser passageiro — repetir).
