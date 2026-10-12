# Projeto: Essência Feminina — Site + Cloud Functions

Versão web (espelho, só fluxo de cliente) do app Android, mais a função que
dispara avisos push pra cabeleireira. Hospedado no Firebase Hosting do
mesmo projeto do app.

- **Firebase project:** `cabeleleiro-ff23d`
- **URL pública:** `https://cabeleleiro-ff23d.web.app`
- **Projeto irmão:** a pasta do app Android (`cabeleleiro`), que tem seu
  próprio CLAUDE.md com a documentação completa da estrutura de dados no
  Firebase. **Leia aquele arquivo também** — a estrutura do banco (schema,
  regras, o bug do `forEach`, o bug da data como chave) é exatamente a
  mesma usada aqui.

## Estrutura da pasta

```
site-agendamento/
  index.html          ← o site inteiro (HTML+CSS+JS num arquivo só,
                          sem build step, sem framework)
  firebase.json        ← config do Hosting + Functions (o "ignore" impede
                          publicar *.md, database.rules.json etc. — não
                          remover, senão vazam no site público)
  database.rules.json  ← cópia versionada das regras do Realtime Database
                          (NÃO está ligada ao deploy; a fonte da verdade
                          continua sendo o Console até isso ser decidido)
  .well-known/
    assetlinks.json     ← prova que o site e o app Android são "parentes",
                          necessário pro App Link do app funcionar
  functions/
    index.js            ← a Cloud Function de aviso push
    package.json
```

## O site (`index.html`)

- Single-file app, sem bundler. Usa o **Firebase JS SDK versão "compat"**
  (carregada via `<script>` do jsDelivr), não os módulos ES modernos —
  escolha deliberada pra simplicidade, sem precisar de npm/build pro site.
- Cobre só o fluxo de **cliente**: login/cadastro, ver serviços, agendar
  (respeitando duração de cada serviço e dias de funcionamento), ver/editar/
  cancelar os próprios agendamentos, ver perfil da profissional, editar o
  próprio perfil. **Não tem** o painel de administração da cabeleireira —
  isso só existe no app Android.
- A lógica de reserva de horário (blocos de 20 min, chave de data sem
  barra, escrita atômica multi-path) é uma **reimplementação em
  JavaScript** da mesma lógica do `BancoController.java` do app. Se uma
  mudar, a outra precisa mudar junto, ou os dois vão divergir no que cada
  um considera "horário livre".
- **Cuidado ao usar `snap.forEach()` do Firebase aqui também** — mesmo
  bug do lado Android: se o callback retornar algo truthy (ex:
  `array.push(x)` sem chaves, que retorna o novo tamanho do array), o
  Firebase para de iterar depois do primeiro item. Sempre:
  `snap.forEach(filho => { lista.push(filho.val()); })` (com chaves).

## Deploy do site

```bash
firebase deploy --only hosting
```

Publica o `index.html` e tudo mais na pasta (exceto o que estiver no
`ignore` do `firebase.json`). **O `firebase.json` já está configurado pra
NÃO ignorar a pasta `.well-known/`** (por padrão o Firebase Hosting ignora
pastas começadas com ponto — isso foi ajustado de propósito, não reverter,
senão o App Link do app Android para de funcionar).

## Deploy da Cloud Function

```bash
cd functions
npm install
cd ..
firebase deploy --only functions
```

### `functions/index.js` — o que ela faz

Dispara (`onValueCreated`) toda vez que um nó novo aparece em
`/agendamentos/{agendamentoId}` — não importa se foi criado pelo app ou
pelo site, porque os dois escrevem no mesmo lugar.

1. Busca em `usuarios` quem tem `tipo == "cabeleireira"` e tem um
   `fcmToken` salvo.
2. Não avisa a própria pessoa que criou o agendamento (caso a cabeleireira
   tenha marcado um encaixe nela mesma).
3. Manda a notificação push (FCM) pra todos os tokens encontrados.
4. Se algum token não existir mais (app desinstalado, usuário trocou de
   conta), remove ele do perfil automaticamente.

`maxInstances: 3` limita quantas cópias da função rodam ao mesmo tempo —
proteção contra custo inesperado em caso de uso anormal.

### Gotchas já resolvidos no deploy (não repetir o processo de debug)

- **Erro "Permission denied... Eventarc Service Agent" na primeira vez
  que a função é criada:** não é erro de configuração, é demora normal de
  propagação de permissões internas do Google na primeira vez que o
  projeto usa functions de 2ª geração. Solução: esperar alguns minutos e
  rodar `firebase deploy --only functions` de novo, sem mudar nada.
- **Erro "Cannot determine backend specification. Timeout after 10000":**
  geralmente causado por versão desatualizada de `firebase-functions` ou
  `firebase-admin` no `package.json`. Rodar
  `npm install --save firebase-functions@latest firebase-admin@latest`
  dentro de `functions/` resolve. Pra depurar se o código em si carrega
  sem erro (sem depender do Firebase), rodar
  `node -e "require('./index.js'); console.log('CARREGOU OK')"`
  dentro da pasta `functions`.
- **`TypeError: admin.database is not a function` nos logs:** a partir do
  `firebase-admin` 14 o jeito antigo (`admin.database()`,
  `admin.messaging()`) não existe mais. Usar os imports separados:
  `require("firebase-admin/app")` (`initializeApp`),
  `require("firebase-admin/database")` (`getDatabase`) e
  `require("firebase-admin/messaging")` (`getMessaging`).
- **Ver os logs:** `firebase functions:log --only avisarNovoAgendamento`
  (o nome é "avisar", com "r"). Se der "Failed to retrieve log entries",
  costuma ser falha passageira — é só rodar de novo.
- **Function exige plano Blaze:** o projeto já está no Blaze. Se algum dia
  aparecer esse erro nesse mesmo projeto, é sinal de que o plano voltou
  pro Spark por algum motivo (verificar em
  Firebase Console → Configurações → Utilização e faturamento).
- **`package.json` da pasta `functions` fixa `"engines": { "node": "22" }`**
  — isso é só uma recomendação pro Firebase (o deploy roda na nuvem do
  Google, não na máquina local), não precisa bater com a versão do Node
  instalada localmente no computador de quem desenvolve.

## App Link (relação com o app Android)

- `.well-known/assetlinks.json` precisa conter o `package_name`
  (`com.example.cabeleleiro`) e o SHA-256 do certificado que assina o APK
  atual.
- Se o app for reassinado com outra chave (ex: publicação na Play Store),
  esse arquivo precisa ganhar o novo SHA-256 na lista
  `sha256_cert_fingerprints` (pode ter mais de um, não precisa remover o
  antigo se ainda for usado em algum APK distribuído).
- Pra conferir se está publicado certo:
  `https://cabeleleiro-ff23d.web.app/.well-known/assetlinks.json` deve
  devolver o JSON puro, não a tela de login do site.

## Regras de segurança

Compartilhadas com o app Android — é o mesmo banco de dados, mesmas
regras. Ver o CLAUDE.md da pasta `cabeleleiro` pra descrição completa. Não
existe regra separada "só pro site".
