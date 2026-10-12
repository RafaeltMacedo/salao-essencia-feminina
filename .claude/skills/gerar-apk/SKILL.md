---
name: gerar-apk
description: Guia para gerar o APK release assinado do app Android (cabeleleiro), atualizar versão e manter o App Link funcionando. Usar quando o usuário quiser gerar, compilar ou distribuir uma nova versão do app.
disable-model-invocation: true
---

# Gerar APK do app

## 1. Versão

Em `cabeleleiro/app/build.gradle.kts`, aumentar **sempre**:
- `versionCode` (+1, número inteiro) — sem isso o Android recusa atualizar
  por cima da versão instalada.
- `versionName` (ex.: `1.0` → `1.1`) — é o que a pessoa vê.

## 2. Chave de assinatura (conferir antes)

`cabeleleiro/assinatura.keystore` e `cabeleleiro/keystore.properties` precisam
existir (ficam fora do git; cópia de segurança no Google Drive do usuário).
Se faltarem, **parar** e pedir para o usuário restaurar do Drive. Nunca gerar
chave nova sem o usuário decidir: obrigaria todas as clientes a reinstalar.
Nunca ler o conteúdo desses arquivos.

## 3. Build (sempre release)

```bash
cd cabeleleiro
./gradlew assembleRelease
```
Saída: `cabeleleiro/app/build/outputs/apk/release/app-release.apk`.
(No Android Studio: Build Variants = release.) Não distribuir `app-debug.apk`.

## 4. Conferir a assinatura

```bash
<sdk>/build-tools/<versão>/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```
O SHA-256 tem que ser `b7a334f2…10edb897`, o mesmo de
`site-agendamento/.well-known/assetlinks.json`. Se for diferente, **parar**:
as clientes não conseguiriam atualizar por cima e o App Link quebraria.

## 5. Distribuir

Para as clientes vai **só** o `app-release.apk`, enviado manualmente (WhatsApp
como Documento ou Drive). A chave (`assinatura.keystore` e
`keystore.properties`) **nunca** vai para cliente. Nunca commitar o `.apk` nem
as chaves; já estão no `.gitignore`.

## 6. Registrar

`git commit` + `git tag app-v<versionName>`; enviar ao GitHub (`git push`,
com confirmação do usuário).
