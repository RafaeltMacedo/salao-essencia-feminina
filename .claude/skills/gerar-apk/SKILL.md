---
name: gerar-apk
description: Guia para gerar o APK do app Android (cabeleleiro), atualizar versão e manter o App Link funcionando. Usar quando o usuário quiser gerar, compilar ou distribuir uma nova versão do app.
disable-model-invocation: true
---

# Gerar APK do app

## 1. Versão

Em `cabeleleiro/app/build.gradle.kts`, aumentar **sempre**:
- `versionCode` (+1, número inteiro) — sem isso o Android recusa atualizar
  por cima da versão instalada.
- `versionName` (ex.: `1.0` → `1.1`) — é o que a pessoa vê.

## 2. Build

Pelo Android Studio (abrir a pasta `cabeleleiro/`):
**Build → Generate App Bundles or APKs → Generate APKs**.

Ou pela linha de comando (precisa do JDK do Android Studio):
```bash
cd cabeleleiro
./gradlew assembleDebug
```
Saída: `cabeleleiro/app/build/outputs/apk/debug/app-debug.apk`.

## 3. App Link (só se a chave de assinatura mudou)

```bash
cd cabeleleiro
./gradlew signingReport
```
Comparar o SHA-256 (bloco `Variant: debug` ou `release`) com
`site-agendamento/.well-known/assetlinks.json`. Se for diferente, **adicionar**
(não substituir) na lista `sha256_cert_fingerprints` e rodar `/deploy-site`.

## 4. Distribuir

O APK é enviado manualmente (WhatsApp como Documento ou Drive).
Nunca commitar o `.apk` nem keystores (`*.jks`, `*.keystore`) — já estão no
`.gitignore`.

## 5. Registrar

`git commit` + `git tag app-v<versionName>`.
