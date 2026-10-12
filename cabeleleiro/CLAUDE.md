# Projeto: Essência Feminina — App Android (cliente + cabeleireira)

App de agendamento para salão de beleza. Dois tipos de usuário (cliente e
cabeleireira) usando a mesma base de código, perfis diferentes conforme
`usuarios/{uid}/tipo`.

- **Package:** `com.example.cabeleleiro`
- **Firebase project:** `cabeleleiro-ff23d`
- **Linguagem:** Java (não Kotlin)
- **Projeto irmão:** a pasta `site-agendamento` (fora deste repo) contém a
  versão web (espelho, só cliente) e as Cloud Functions. Tem seu próprio
  CLAUDE.md.

## Antes de mexer em qualquer coisa

Este projeto já passou por bastante iteração e teve bugs sutis corrigidos
com esforço. **Não "corrija" os pontos abaixo sem entender o motivo primeiro**
— eles são intencionais:

1. **`snap.forEach()` do Firebase nunca pode retornar valor verdadeiro
   implicitamente.** Se o callback de um `DataSnapshot.forEach` retornar
   `true` (por exemplo, `list.add(x)` do Java retorna boolean, ou no lado
   JS `array.push()` retorna um número truthy), o Firebase **interrompe a
   iteração na primeira vez que isso acontecer**. Isso já causou um bug real
   onde só o primeiro item de uma lista aparecia. Sempre garanta que o
   callback não devolve nada (bloco `{ }` no Java já resolve, já que métodos
   void não retornam nada).

2. **Datas nunca podem virar "pasta" direto no Firebase.** O formato usado em
   todo o app é `dd/MM/yyyy` (ex: `22/09/2026`), e a barra `/` é separador de
   caminho no Firebase — usar a data direto como `child(data)` faz o
   Realtime Database criar 3 níveis aninhados em vez de um nó só, quebrando
   as regras de segurança. Sempre que a data for usada como **chave** (não
   como valor exibido), passa por `BancoController.chaveData()`
   (troca `/` por `-`). Isso afeta principalmente o nó `horarios_ocupados`.

3. **Botões desabilitados durante operação assíncrona precisam ser
   reabilitados ao reabrir a tela**, não só no callback de sucesso/erro. Como
   várias telas reaproveitam a mesma instância de Activity/elemento, um botão
   desabilitado numa tentativa anterior pode ficar travado pra sempre se a
   pessoa voltar pra mesma tela sem o app recarregar do zero.

4. **Cadastro público sempre cria `tipo: "cliente"`.** Não existe mais opção
   de escolher "cabeleireira" no formulário de cadastro — isso foi removido
   de propósito (falha de segurança: qualquer pessoa virava admin do salão).
   A única cabeleireira (conta da Fernanda) foi promovida manualmente, uma
   vez, editando o campo `tipo` direto no Firebase Console. As regras do
   banco (`.validate` em `usuarios/$uid/tipo`) bloqueiam qualquer tentativa
   de mudar esse campo depois de criado, exceto pela própria cabeleireira
   através das regras normais — na prática, só edição manual no Console
   consegue promover alguém.

## Estrutura de dados no Firebase Realtime Database

```
usuarios/{uid}
  nome, telefone, email, tipo ("cliente" | "cabeleireira"), fcmToken

agendamentos/{agendamentoId}
  clienteUid, nomeCliente, telefoneCliente, servico (texto já formatado,
  ex: "Corte — R$ 50,00"), data ("dd/MM/yyyy"), hora ("HH:mm"),
  status ("pendente" | "concluido"), criadoEm (timestamp),
  duracaoMinutos (duração do serviço NO MOMENTO do agendamento — não muda
  se o serviço for editado depois)

agendamentos_por_cliente/{clienteUid}/{agendamentoId}
  CÓPIA EXATA do nó acima. Existe porque o Firebase Realtime Database não
  consegue aplicar regra de leitura "cliente só vê os próprios" numa LISTA
  (query) — só em leitura de um nó específico. Então cada agendamento é
  gravado duas vezes, sempre na mesma escrita atômica (multi-path update).
  A cabeleireira lê `agendamentos` (lista completa); a cliente lê só a
  própria pasta em `agendamentos_por_cliente/{seu uid}`.

horarios_ocupados/{chaveData}/{bloco}
  valor = uid de quem reservou. `chaveData` é a data com "-" no lugar de
  "/" (ver ponto 2 acima). `bloco` é um horário "HH:mm" alinhado à
  granularidade de 20 minutos (GRANULARIDADE_MINUTOS em BancoController).
  Um serviço de 90 min reserva 5 blocos consecutivos (arredonda pra cima).
  A reserva é sempre atômica: todos os blocos necessários são escritos (ou
  liberados) numa única operação multi-path, e a regra do Firebase só
  permite criar um bloco se ele ainda não existir — é isso que impede duas
  pessoas reservando horários sobrepostos ao mesmo tempo.

servicos/{id}
  nome, preco, duracaoMinutos

configuracoes/diasFuncionamento
  domingo, segunda, terca, quarta, quinta, sexta, sabado (boolean, padrão
  true se não existir)
```

## Regras de segurança (Realtime Database)

O arquivo de regras publicado está documentado e versionado fora deste repo
(peça pro usuário mandar o conteúdo atual do Firebase Console se precisar
conferir). Pontos-chave:

- `usuarios/$uid`: só o próprio dono lê/escreve. O campo `tipo` tem
  `.validate` que impede autopromoção pra `cabeleireira`.
- `agendamentos`: leitura da lista completa só pra quem tem
  `tipo == "cabeleireira"`. Escrita por item: cabeleireira sempre pode;
  cliente só pode criar com `clienteUid == auth.uid`, e só pode
  editar/excluir o que já é dela.
- `agendamentos_por_cliente/$clienteUid`: cada cliente só lê/escreve a
  própria subárvore.
- `horarios_ocupados/$data/$hora`: escrita permitida se o bloco ainda não
  existe, ou se quem está escrevendo é o dono original do bloco, ou se é a
  cabeleireira.
- `servicos` e `configuracoes`: leitura livre pra qualquer autenticado,
  escrita só pela cabeleireira.

## Avisos push (Firebase Cloud Messaging)

- `PushService.java` (extends `FirebaseMessagingService`) recebe os
  avisos e também salva o token do aparelho em `usuarios/{uid}/fcmToken`.
- `MenuCabeleireira.java` registra o token ao entrar (chama
  `FirebaseMessaging.getInstance().getToken()`), e remove o token
  (`deleteToken()`) ao sair.
- Quem dispara o aviso é a Cloud Function `avisarNovoAgendamento`, que vive
  no projeto `site-agendamento/functions` (não neste repo). Ela dispara
  quando um nó novo é criado em `agendamentos/{id}`, não importa se foi
  pelo app ou pelo site.
- Existia antes um serviço de monitoramento em primeiro plano
  (`NovoAgendamentoService`) que fazia algo parecido sem custo, mas foi
  substituído pelo FCM (mais confiável, funciona com o app fechado). O
  arquivo antigo pode ainda existir no projeto, mas não é mais chamado.

## App Links

- O app abre automaticamente quando alguém toca em
  `https://cabeleleiro-ff23d.web.app` (configurado em `AndroidManifest.xml`,
  intent-filter com `android:autoVerify="true"` no `Login`).
- A verificação depende de um arquivo `assetlinks.json` publicado no mesmo
  domínio (está no projeto `site-agendamento`, pasta `.well-known/`), com o
  SHA-256 do certificado que assina o APK.
- **Se o certificado de assinatura mudar** (por exemplo, ao publicar na Play
  Store, que assina com chave própria), o novo SHA-256 precisa ser
  adicionado à lista `sha256_cert_fingerprints` do `assetlinks.json`, senão
  o link para de abrir o app direto.
- Pra pegar o SHA-256 do keystore de debug atual: `gradlew signingReport`
  (procurar o bloco `Variant: debug`).

## Identidade visual

- Paleta: fundo preto (`#0D0D0D`), cards cinza escuro (`#1C1C1C`), dourado
  (`#C9A227`) como cor de destaque/ação.
- Fontes: título em itálico serifado (estilo elegante), corpo em sans-serif.
- Ícone do app: tesoura dourada sobre fundo preto redondo/quadrado
  arredondado, desenhado manualmente (não é uma foto). Fica em
  `mipmap-*dpi/ic_launcher*.webp` + ícone adaptativo referenciando
  `mipmap/ic_launcher_foreground` e `mipmap/ic_launcher_background`.
- Foto usada na tela de login e na tela "Profissional" é um recorte da
  imagem de divulgação real da cabeleireira (sem o texto do panfleto
  original).

## Build

- Abrir a pasta `cabeleleiro` (a que tem `build.gradle.kts` direto dentro)
  no Android Studio.
- Gerar APK: **Build → Generate App Bundles or APKs → Generate APKs**.
- O usuário distribui o APK manualmente (WhatsApp como Documento, ou link
  do Drive) — o app ainda não está na Play Store.
- `minSdk` relativamente baixo, mas as funcionalidades mais recentes
  (notificação, permissão) já tratam Android 13+ (`POST_NOTIFICATIONS`)
  corretamente com checagem de versão.

## O que já foi decidido e não precisa ser revisitado

- Notificação de "novo agendamento" pra cabeleireira usa Cloud Functions +
  FCM (plano Blaze ativo). Decisão consciente do usuário, sabendo que exige
  cartão de crédito vinculado (mas custo esperado é R$ 0 pro volume dele).
- Granularidade de horário é 20 minutos, serviços podem durar qualquer
  múltiplo disso (arredondado pra cima).
- Não existe edição de e-mail/senha pelo Perfil (só nome/telefone) — não foi
  pedido ainda.
- Publicação na Play Store foi adiada de propósito (fase de teste primeiro).
