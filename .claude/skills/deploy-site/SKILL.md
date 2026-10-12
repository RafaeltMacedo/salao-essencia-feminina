---
name: deploy-site
description: Publica o site de agendamento (site-agendamento/index.html) no Firebase Hosting com checagens de segurança antes e depois. Usar quando o usuário pedir para publicar, subir ou fazer deploy do site.
disable-model-invocation: true
---

# Deploy do site (Firebase Hosting)

Projeto: `cabeleleiro-ff23d` — URL: https://cabeleleiro-ff23d.web.app

## Antes de publicar (parar se algo falhar)

1. `site-agendamento/index.html` existe e não está vazio.
2. `git status` limpo, ou mostrar ao usuário o que vai ao ar sem commit.
3. `firebase.json`:
   - `"ignore"` contém `**/*.md`, `database.rules.json`, `.claude/**`,
     `**/firebase.json`, `functions/**`, `testes-regras/**`.
   - `.well-known` **não** está ignorado.
4. `.well-known/assetlinks.json` é JSON válido e tem
   `package_name: com.example.cabeleleiro`.
5. Se `index.html` mudou lógica de reserva, conferir
   `.claude/rules/sincronia-app-site.md` (o app precisa da mesma mudança?).
6. Mostrar o resumo do que vai ser publicado e **pedir confirmação**.

## Publicar

```bash
cd site-agendamento
firebase deploy --only hosting
```

Só o site. Regras do banco são outra publicação (`--only database`), com os
testes de `testes-regras/` antes. Nunca `firebase deploy` sem `--only`.

## Depois

- `https://cabeleleiro-ff23d.web.app/` responde 200.
- `https://cabeleleiro-ff23d.web.app/.well-known/assetlinks.json` devolve
  JSON puro (não HTML).
- Arquivos internos **não** podem estar publicados. Cada endereço abaixo tem
  que devolver o index.html do site (o rewrite), nunca o arquivo de verdade:
  `/CLAUDE.md`, `/database.rules.json`, `/firebase.json`,
  `/.well-known/firebase.json`, `/package-lock.json`, `/.gitignore`,
  `/functions/index.js`, `/testes-regras/regras.test.js`.
  Se algum vazar, avisar o usuário na hora.
- O deploy deve listar poucos arquivos ("found N files"): hoje são 2
  (`index.html` e `.well-known/assetlinks.json`). Número maior = conferir o `ignore`.
- Sugerir commit/tag: `git tag site-AAAA-MM-DD`.
