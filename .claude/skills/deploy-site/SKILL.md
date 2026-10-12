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
   - `"ignore"` contém `**/*.md`, `database.rules.json`, `.claude/**`.
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

## Depois

- `https://cabeleleiro-ff23d.web.app/` responde 200.
- `https://cabeleleiro-ff23d.web.app/.well-known/assetlinks.json` devolve
  JSON puro (não HTML).
- `https://cabeleleiro-ff23d.web.app/CLAUDE.md` **não** deve devolver o
  arquivo (o rewrite devolve o index.html — conferir que o conteúdo não é o
  markdown).
- Sugerir commit/tag: `git tag site-AAAA-MM-DD`.
