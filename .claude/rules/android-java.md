---
paths:
  - "cabeleleiro/**/*.java"
  - "cabeleleiro/**/*.xml"
  - "cabeleleiro/**/*.kts"
---

# Convenções do app Android

- Linguagem é **Java** (Java 8 / `sourceCompatibility 1_8`). Não converter
  para Kotlin nem introduzir Kotlin sem o usuário pedir.
- `minSdk 24`, `targetSdk 34`: recursos de API mais nova precisam de checagem
  `Build.VERSION.SDK_INT` (como já é feito para `POST_NOTIFICATIONS`).
- Cores e textos: usar `res/values/colors.xml` e `strings.xml`, não valores
  fixos no layout. Paleta: fundo `#0D0D0D`, card `#1C1C1C`, dourado `#C9A227`.
- Botões desabilitados durante operação assíncrona devem ser reabilitados ao
  reabrir a tela (`onResume`), não só no callback.
- Novas dependências entram em `gradle/libs.versions.toml`, não com versão
  fixa no `app/build.gradle.kts`.
- Não é possível compilar daqui sem o Android Studio/JDK configurados; se o
  build falhar por ambiente, dizer isso em vez de "consertar" o código.
