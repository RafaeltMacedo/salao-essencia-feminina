/**
 * Avisa a cabeleireira (push do Firebase Cloud Messaging) sempre que um
 * agendamento novo é criado, pelo app ou pelo site.
 *
 * Cuidados de custo: a função só LÊ e manda o aviso, e a única escrita que
 * faz (limpar aparelhos antigos) é em /usuarios, um caminho diferente do que
 * dispara ela (/agendamentos). Por isso não existe risco de ficar em laço.
 */
const {onValueCreated} = require("firebase-functions/v2/database");
const {logger} = require("firebase-functions");
const {initializeApp} = require("firebase-admin/app");
const {getDatabase} = require("firebase-admin/database");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();

exports.avisarNovoAgendamento = onValueCreated(
    {
      ref: "/agendamentos/{agendamentoId}",
      instance: "cabeleleiro-ff23d-default-rtdb",
      region: "us-central1",
      maxInstances: 3, // limita quantas cópias rodam ao mesmo tempo (protege o custo)
    },
    async (event) => {
      const agendamento = event.data.val();
      if (!agendamento) {
        return;
      }

      // Procura as contas do tipo "cabeleireira" que têm um aparelho registrado
      const snapshot = await getDatabase()
          .ref("usuarios")
          .orderByChild("tipo")
          .equalTo("cabeleireira")
          .once("value");

      const tokens = [];
      const donoDoToken = {};
      snapshot.forEach((filho) => {
        const usuario = filho.val() || {};
        // Se foi a própria cabeleireira que marcou (encaixe), não avisa ela de si mesma
        if (filho.key !== agendamento.clienteUid && usuario.fcmToken) {
          tokens.push(usuario.fcmToken);
          donoDoToken[usuario.fcmToken] = filho.key;
        }
      });

      if (tokens.length === 0) {
        logger.info("Nenhuma cabeleireira com aparelho registrado, nada a enviar.");
        return;
      }

      const resposta = await getMessaging().sendEachForMulticast({
        tokens: tokens,
        notification: {
          title: "Novo agendamento!",
          body: `${agendamento.nomeCliente} marcou ${agendamento.servico} ` +
              `para ${agendamento.data} às ${agendamento.hora}.`,
        },
        data: {agendamentoId: event.params.agendamentoId},
        android: {
          priority: "high",
          notification: {channelId: "lembretes_agendamento"},
        },
      });

      // Limpa aparelhos que não existem mais (app desinstalado, ela saiu da conta etc.)
      const limpezas = [];
      resposta.responses.forEach((r, i) => {
        const codigo = r.error && r.error.code;
        if (!r.success && (codigo === "messaging/registration-token-not-registered" ||
            codigo === "messaging/invalid-registration-token")) {
          limpezas.push(getDatabase()
              .ref(`usuarios/${donoDoToken[tokens[i]]}/fcmToken`).remove());
        }
      });
      await Promise.all(limpezas);

      logger.info(`Aviso enviado: ${resposta.successCount} ok, ${resposta.failureCount} com falha.`);
    },
);
