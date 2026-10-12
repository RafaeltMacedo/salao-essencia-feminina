/* =========================================================================
   Testes das regras do Realtime Database (database.rules.json)

   Roda num banco de TESTE local (emulador do Firebase), nunca em produção.
   Simula as mesmas gravações que o app Android e o site fazem, e também
   tentativas de abuso, e confere se cada uma é aceita ou recusada.

   Como rodar (de dentro da pasta site-agendamento):
     firebase emulators:exec --only database --project demo-teste "node testes-regras/regras.test.js"

   Rodar SEMPRE antes de publicar mudança nas regras.
   ========================================================================= */

const BASE = 'http://127.0.0.1:9000'; // porta padrão do emulador do Realtime Database
// O emulador aplica as regras ao banco "<projeto>-default-rtdb"
const NS = '?ns=demo-teste-default-rtdb';

// Login simulado: token sem assinatura, que o emulador aceita.
// Vai no parâmetro "auth" da URL (no cabeçalho, o emulador trataria como administrador).
function token(uid) {
  const b64 = (o) => Buffer.from(JSON.stringify(o)).toString('base64url');
  const agora = Math.floor(Date.now() / 1000);
  return b64({ alg: 'none', typ: 'JWT' }) + '.' + b64({
    sub: uid, user_id: uid, iat: agora, exp: agora + 3600, auth_time: agora,
    aud: 'demo-teste', iss: 'https://securetoken.google.com/demo-teste',
    firebase: { sign_in_provider: 'password' },
  }) + '.';
}

// uid = 'ADMIN' grava ignorando as regras (só para preparar os dados do teste);
// uid = null grava sem login
async function gravar(metodo, caminho, corpo, uid) {
  let url = BASE + caminho + '.json' + NS;
  const headers = {};
  if (uid === 'ADMIN') headers.Authorization = 'Bearer owner';
  else if (uid) url += '&auth=' + token(uid);
  const resposta = await fetch(url, { method: metodo, headers, body: corpo === undefined ? undefined : JSON.stringify(corpo) });
  return resposta.ok;
}

let falhas = 0;
async function esperado(descricao, deveSerPermitido, promessa) {
  const permitido = await promessa;
  const passou = permitido === deveSerPermitido;
  if (!passou) falhas++;
  console.log(`${passou ? 'OK   ' : 'FALHA'} ${descricao} (permitido=${permitido})`);
}

async function horariosOcupados() {
  const H = '/horarios_ocupados/12-10-2026';
  await gravar('PUT', '/usuarios', { ana: { tipo: 'cliente' }, bia: { tipo: 'cliente' }, dona: { tipo: 'cabeleireira' } }, 'ADMIN');
  await esperado('cliente reserva horário livre com o próprio uid', true, gravar('PUT', H + '/09:00', 'ana', 'ana'));
  await esperado('cliente reserva horário livre com uid de outra', false, gravar('PUT', H + '/09:20', 'bia', 'ana'));
  await esperado('cliente reserva horário livre com valor inválido', false, gravar('PUT', H + '/09:40', 'xyz', 'ana'));
  await esperado('cliente grava número em vez de uid', false, gravar('PUT', H + '/10:00', 123, 'ana'));
  await esperado('outra cliente toma horário ocupado', false, gravar('PUT', H + '/09:00', 'bia', 'bia'));
  await esperado('outra cliente apaga horário alheio', false, gravar('DELETE', H + '/09:00', undefined, 'bia'));
  await esperado('dona do horário regrava (edição)', true, gravar('PUT', H + '/09:00', 'ana', 'ana'));
  await esperado('dona do horário libera', true, gravar('DELETE', H + '/09:00', undefined, 'ana'));
  await esperado('sem login não grava', false, gravar('PUT', H + '/11:00', 'ana', null));
  await esperado('cabeleireira marca horário para cliente', true, gravar('PUT', H + '/12:00', 'ana', 'dona'));
  await esperado('cabeleireira libera horário de cliente', true, gravar('DELETE', H + '/12:00', undefined, 'dona'));
  await esperado('reserva de vários blocos de uma vez', true, gravar('PATCH', '/', { [H + '/14:00']: 'bia', [H + '/14:20']: 'bia' }, 'bia'));
  await esperado('vários blocos com um já ocupado falha inteiro', false, gravar('PATCH', '/', { [H + '/14:20']: 'ana', [H + '/14:40']: 'ana' }, 'ana'));
  await esperado('edição: libera antigo e ocupa novo', true, gravar('PATCH', '/', { [H + '/14:00']: null, [H + '/15:00']: 'bia' }, 'bia'));
}

async function perfis() {
  await gravar('PUT', '/usuarios', { ana: { tipo: 'cliente', nome: 'Ana' }, dona: { tipo: 'cabeleireira', nome: 'Dona' } }, 'ADMIN');
  await esperado('cliente atualiza o nome', true, gravar('PATCH', '/usuarios/ana', { nome: 'Ana Maria' }, 'ana'));
  await esperado('cliente grava token de notificação', true, gravar('PUT', '/usuarios/ana/fcmToken', 'tok', 'ana'));
  await esperado('cliente lê perfil de outra pessoa', false, fetch(BASE + '/usuarios/dona.json' + NS + '&auth=' + token('ana')).then(r => r.ok));
  await esperado('cliente apaga o próprio tipo', false, gravar('DELETE', '/usuarios/ana/tipo', undefined, 'ana'));
  await esperado('perfil gravado sem tipo (apagaria o tipo)', false, gravar('PUT', '/usuarios/ana', { nome: 'Ana' }, 'ana'));
  await esperado('cliente se promove a cabeleireira', false, gravar('PUT', '/usuarios/ana/tipo', 'cabeleireira', 'ana'));
  await esperado('cabeleireira perde o tipo por gravação sem tipo', false, gravar('PUT', '/usuarios/dona', { nome: 'Dona' }, 'dona'));
  await esperado('cabeleireira atualiza o nome', true, gravar('PATCH', '/usuarios/dona', { nome: 'Dona F.' }, 'dona'));
  await esperado('cadastro novo como cliente', true, gravar('PUT', '/usuarios/nova', { nome: 'Nova', tipo: 'cliente' }, 'nova'));
  await esperado('cadastro novo já como cabeleireira', false, gravar('PUT', '/usuarios/nova2', { nome: 'X', tipo: 'cabeleireira' }, 'nova2'));
  await esperado('token gravado antes do perfil (conta nova)', true, gravar('PUT', '/usuarios/nova3/fcmToken', 'tok', 'nova3'));
  await esperado('perfil gravado depois do token, como cliente', true, gravar('PATCH', '/usuarios/nova3', { nome: 'N3', tipo: 'cliente' }, 'nova3'));
}

async function agendamentos() {
  await gravar('PUT', '/usuarios', { ana: { tipo: 'cliente' }, bia: { tipo: 'cliente' }, dona: { tipo: 'cabeleireira' } }, 'ADMIN');
  await gravar('PUT', '/horarios_ocupados', null, 'ADMIN');
  const H = '/horarios_ocupados/20-10-2026/';
  const ag = (uid, hora, extra) => ({
    clienteUid: uid, nomeCliente: 'N', telefoneCliente: '', servico: 'Corte — R$ 50,00',
    data: '20/10/2026', hora, status: 'pendente', criadoEm: 1, duracaoMinutos: 40, ...extra,
  });
  // Igual ao app/site: agendamento + cópia da cliente + blocos de horário, numa gravação só
  const criar = (id, uid, hora, extra, blocos) => {
    const u = { ['/agendamentos/' + id]: ag(uid, hora, extra), ['/agendamentos_por_cliente/' + uid + '/' + id]: ag(uid, hora, extra) };
    (blocos || []).forEach(b => { u[H + b] = uid; });
    return gravar('PATCH', '/', u, uid);
  };
  const editar = (id, uid, campos, extra) => {
    const u = { ...extra };
    for (const [k, v] of Object.entries(campos)) {
      u['/agendamentos/' + id + '/' + k] = v;
      u['/agendamentos_por_cliente/' + uid + '/' + id + '/' + k] = v;
    }
    return gravar('PATCH', '/', u, uid);
  };

  await esperado('cria agendamento reservando o horário junto', true, criar('a1', 'ana', '10:00', null, ['10:00', '10:20']));
  await esperado('cria agendamento SEM reservar horário', false, criar('a2', 'ana', '11:00', null, []));
  await esperado('cria agendamento já como concluído', false, criar('a3', 'ana', '12:00', { status: 'concluido' }, ['12:00', '12:20']));
  await esperado('cria em cima do horário de outra cliente', false, criar('a4', 'bia', '10:00', null, ['10:00', '10:20']));
  await esperado('cria apontando para horário reservado por outra', false, gravar('PATCH', '/', { '/agendamentos/a5': ag('bia', '10:00') }, 'bia'));
  await esperado('cliente lê a lista de todos os agendamentos', false, fetch(BASE + '/agendamentos.json' + NS + '&auth=' + token('ana')).then(r => r.ok));
  await esperado('edita nome/telefone sem mudar horário', true, editar('a1', 'ana', { nomeCliente: 'Ana M', telefoneCliente: '1199' }));
  await esperado('muda horário (libera antigo, reserva novo)', true, editar('a1', 'ana', { hora: '14:00' }, { [H + '10:00']: null, [H + '10:20']: null, [H + '14:00']: 'ana', [H + '14:20']: 'ana' }));
  await esperado('muda horário SEM reservar o novo', false, editar('a1', 'ana', { hora: '16:00' }));
  await esperado('cliente marca o próprio como concluído', false, editar('a1', 'ana', { status: 'concluido' }));
  await esperado('cliente passa o agendamento para outra', false, gravar('PATCH', '/', { '/agendamentos/a1/clienteUid': 'bia' }, 'ana'));
  await esperado('outra cliente edita agendamento alheio', false, gravar('PATCH', '/', { '/agendamentos/a1/nomeCliente': 'X' }, 'bia'));
  await esperado('cabeleireira lê a lista de todos os agendamentos', true, fetch(BASE + '/agendamentos.json' + NS + '&auth=' + token('dona')).then(r => r.ok));
  await esperado('cabeleireira marca como concluído', true, gravar('PATCH', '/', { '/agendamentos/a1/status': 'concluido', '/agendamentos_por_cliente/ana/a1/status': 'concluido' }, 'dona'));
  await esperado('cliente edita agendamento já concluído (status igual)', true, editar('a1', 'ana', { telefoneCliente: '1188' }));
  await esperado('cabeleireira cria encaixe para ela mesma', true, criar('a6', 'dona', '17:00', null, ['17:00', '17:20']));
  await esperado('cliente cancela o próprio agendamento', true, gravar('DELETE', '/agendamentos/a1', undefined, 'ana'));
}

async function servicosEConfiguracoes() {
  await gravar('PUT', '/usuarios', { ana: { tipo: 'cliente' }, dona: { tipo: 'cabeleireira' } }, 'ADMIN');
  await esperado('cliente altera preço de serviço', false, gravar('PUT', '/servicos/s1', { nome: 'Corte', preco: 1, duracaoMinutos: 40 }, 'ana'));
  await esperado('cabeleireira cadastra serviço', true, gravar('PUT', '/servicos/s1', { nome: 'Corte', preco: 50, duracaoMinutos: 40 }, 'dona'));
  await esperado('cliente lê serviços', true, fetch(BASE + '/servicos.json' + NS + '&auth=' + token('ana')).then(r => r.ok));
  await esperado('cliente altera dias de funcionamento', false, gravar('PUT', '/configuracoes/diasFuncionamento/domingo', true, 'ana'));
  await esperado('cabeleireira altera dias de funcionamento', true, gravar('PUT', '/configuracoes/diasFuncionamento/domingo', false, 'dona'));
}

(async () => {
  console.log('--- horários ocupados');  await horariosOcupados();
  console.log('--- perfis');             await perfis();
  console.log('--- agendamentos');       await agendamentos();
  console.log('--- serviços e configurações'); await servicosEConfiguracoes();
  console.log(falhas ? `\n${falhas} FALHA(S)` : '\nTODOS OS TESTES PASSARAM');
  process.exit(falhas ? 1 : 0);
})();
