package com.example.cabeleleiro;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Controller responsável por todo o CRUD de agendamentos no Firebase.
 *
 * COMO FUNCIONA A RESERVA DE HORÁRIO (com duração variável):
 * A agenda é dividida em blocos fixos de GRANULARIDADE_MINUTOS (20 min).
 * Um serviço de 90 minutos, por exemplo, precisa de 5 blocos seguidos
 * (arredondando pra cima). Ao reservar, o app grava TODOS os blocos
 * necessários de uma vez, numa única operação — se qualquer um desses
 * blocos já estiver ocupado por outra pessoa, a reserva inteira falha
 * (nenhum bloco fica "pela metade" reservado). Isso é o que impede duas
 * clientes ocupando horários que se sobrepõem, mesmo com durações
 * diferentes.
 *
 * Cada agendamento é gravado em DOIS lugares:
 *  - agendamentos/{id}: lista completa, só a cabeleireira lê inteira.
 *  - agendamentos_por_cliente/{uidDaCliente}/{id}: cópia na "pasta" de
 *    cada cliente — ela só lê a própria pasta, nunca a de outra pessoa.
 *
 *  - horarios_ocupados/{data}/{blocoHHmm} = uid de quem reservou, sem
 *    nome nem telefone. Qualquer pessoa logada pode ler isso pra montar
 *    a grade de horários livres.
 */
public class BancoController {

    public static final int GRANULARIDADE_MINUTOS = 20;

    public interface OnAgendamentosListener {
        void onSucesso(List<Agendamento> lista);
        void onErro(String erro);
    }

    public interface OnResultadoListener {
        void onSucesso(String mensagem, String agendamentoId);
        void onErro(String erro);
    }

    public interface OnHorariosOcupadosListener {
        void onSucesso(Set<String> horariosOcupados);
        void onErro(String erro);
    }

    private final DatabaseReference mRaiz;

    public BancoController(android.content.Context context) {
        mRaiz = FirebaseDatabase.getInstance().getReference();
    }

    /** Converte "HH:mm" em minutos desde meia-noite. */
    public static int paraMinutos(String horaTexto) {
        String[] partes = horaTexto.split(":");
        return Integer.parseInt(partes[0]) * 60 + Integer.parseInt(partes[1]);
    }

    /** Converte minutos desde meia-noite em "HH:mm". */
    public static String paraTexto(int minutos) {
        return String.format(Locale.getDefault(), "%02d:%02d", minutos / 60, minutos % 60);
    }

    /**
     * A data (ex: "22/09/2026") tem barras, e barra é caractere especial de
     * caminho no Firebase — usar ela direto como nome de "pasta" faz o
     * Firebase entender como três pastas aninhadas, bagunçando a estrutura.
     * Essa função troca "/" por "-" só para uso como CHAVE interna
     * (22-09-2026); a data continua aparecendo normal ("22/09/2026") em
     * qualquer lugar que o usuário vê na tela.
     */
    public static String chaveData(String data) {
        return data.replace("/", "-");
    }

    /** Quantos blocos de GRANULARIDADE_MINUTOS um serviço ocupa (sempre arredondando pra cima). */
    public static int quantidadeDeBlocos(int duracaoMinutos) {
        return (int) Math.ceil(duracaoMinutos / (double) GRANULARIDADE_MINUTOS);
    }

    /** Lista de horários "HH:mm" ocupados por um agendamento, a partir do horário de início. */
    public static List<String> gerarBlocos(String horaInicio, int duracaoMinutos) {
        List<String> blocos = new ArrayList<>();
        int minutoInicio = paraMinutos(horaInicio);
        int quantidade = quantidadeDeBlocos(duracaoMinutos);
        for (int i = 0; i < quantidade; i++) {
            blocos.add(paraTexto(minutoInicio + i * GRANULARIDADE_MINUTOS));
        }
        return blocos;
    }

    /** READ — quais blocos de horário já estão ocupados num dia (sem expor nome/telefone de ninguém). */
    public void listarHorariosOcupados(String data, OnHorariosOcupadosListener listener) {
        mRaiz.child("horarios_ocupados").child(chaveData(data)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> ocupados = new HashSet<>();
                for (DataSnapshot filho : snapshot.getChildren()) {
                    ocupados.add(filho.getKey());
                }
                listener.onSucesso(ocupados);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onErro(error.getMessage());
            }
        });
    }

    /** CREATE — cria o agendamento (nas duas cópias) E reserva TODOS os blocos que a duração exige, atomicamente. */
    public void insereDados(String clienteUid, String nome, String telefone, String servico,
                             String data, String hora, int duracaoMinutos, OnResultadoListener listener) {
        String agendamentoId = mRaiz.child("agendamentos").push().getKey();
        if (agendamentoId == null) {
            listener.onErro("Erro ao gerar código do agendamento.");
            return;
        }

        Agendamento agendamento = new Agendamento(clienteUid, nome, telefone, servico, data, hora,
                Agendamento.STATUS_PENDENTE);
        agendamento.setCriadoEm(System.currentTimeMillis());
        agendamento.setDuracaoMinutos(duracaoMinutos);

        Map<String, Object> atualizacoes = new HashMap<>();
        atualizacoes.put("/agendamentos/" + agendamentoId, agendamento);
        atualizacoes.put("/agendamentos_por_cliente/" + clienteUid + "/" + agendamentoId, agendamento);
        for (String bloco : gerarBlocos(hora, duracaoMinutos)) {
            atualizacoes.put("/horarios_ocupados/" + chaveData(data) + "/" + bloco, clienteUid);
        }

        mRaiz.updateChildren(atualizacoes)
                .addOnSuccessListener(unused -> listener.onSucesso("Agendamento realizado com sucesso!", agendamentoId))
                .addOnFailureListener(e -> listener.onErro(
                        "Esse horário acabou de ser ocupado por outra pessoa (ou não cabe inteiro antes de outro agendamento). Escolha outro horário."));
    }

    /** UPDATE — altera um agendamento (nas duas cópias), liberando os blocos antigos e reservando os novos, atomicamente. */
    public void atualizarDados(String agendamentoId, String nome, String telefone, String servico,
                                String dataAntiga, String horaAntiga, int duracaoAntiga,
                                String dataNova, String horaNova, int duracaoNova,
                                String clienteUid, OnResultadoListener listener) {
        Map<String, Object> valoresAgendamento = new HashMap<>();
        valoresAgendamento.put("nomeCliente", nome);
        valoresAgendamento.put("telefoneCliente", telefone);
        valoresAgendamento.put("servico", servico);
        valoresAgendamento.put("data", dataNova);
        valoresAgendamento.put("hora", horaNova);
        valoresAgendamento.put("duracaoMinutos", duracaoNova);

        Map<String, Object> atualizacoes = new HashMap<>();
        for (Map.Entry<String, Object> entrada : valoresAgendamento.entrySet()) {
            atualizacoes.put("/agendamentos/" + agendamentoId + "/" + entrada.getKey(), entrada.getValue());
            atualizacoes.put("/agendamentos_por_cliente/" + clienteUid + "/" + agendamentoId + "/" + entrada.getKey(), entrada.getValue());
        }

        boolean mudouHorario = !dataAntiga.equals(dataNova) || !horaAntiga.equals(horaNova) || duracaoAntiga != duracaoNova;
        if (mudouHorario) {
            // Libera todos os blocos antigos e reserva todos os blocos novos, na mesma escrita atômica
            for (String bloco : gerarBlocos(horaAntiga, duracaoAntiga)) {
                atualizacoes.put("/horarios_ocupados/" + chaveData(dataAntiga) + "/" + bloco, null);
            }
            for (String bloco : gerarBlocos(horaNova, duracaoNova)) {
                atualizacoes.put("/horarios_ocupados/" + chaveData(dataNova) + "/" + bloco, clienteUid);
            }
        }

        mRaiz.updateChildren(atualizacoes)
                .addOnSuccessListener(unused -> listener.onSucesso("Agendamento atualizado com sucesso!", agendamentoId))
                .addOnFailureListener(e -> listener.onErro(
                        mudouHorario
                                ? "Esse novo horário acabou de ser ocupado por outra pessoa (ou não cabe inteiro). Escolha outro."
                                : "Erro ao atualizar: " + e.getMessage()));
    }

    /** UPDATE — marca um agendamento como concluído nas duas cópias (não mexe na reserva de horário). */
    public void marcarConcluido(String agendamentoId, String clienteUid, OnResultadoListener listener) {
        Map<String, Object> atualizacoes = new HashMap<>();
        atualizacoes.put("/agendamentos/" + agendamentoId + "/status", Agendamento.STATUS_CONCLUIDO);
        atualizacoes.put("/agendamentos_por_cliente/" + clienteUid + "/" + agendamentoId + "/status", Agendamento.STATUS_CONCLUIDO);

        mRaiz.updateChildren(atualizacoes)
                .addOnSuccessListener(unused -> listener.onSucesso("Agendamento marcado como concluído!", agendamentoId))
                .addOnFailureListener(e -> listener.onErro("Erro ao atualizar status: " + e.getMessage()));
    }

    /** DELETE — cancela o agendamento (nas duas cópias) E libera todos os blocos reservados. */
    public void excluirDados(String agendamentoId, String data, String hora, int duracaoMinutos,
                              String clienteUid, OnResultadoListener listener) {
        // Passo 1: apaga da lista completa (agendamentos/{id})
        mRaiz.child("agendamentos").child(agendamentoId).removeValue()
                .addOnSuccessListener(unused1 -> {
                    // Passo 2: apaga da pasta pessoal da cliente
                    mRaiz.child("agendamentos_por_cliente").child(clienteUid).child(agendamentoId).removeValue()
                            .addOnSuccessListener(unused2 -> {
                                // Passo 3: libera todos os blocos de horário que esse agendamento ocupava
                                Map<String, Object> liberarBlocos = new HashMap<>();
                                for (String bloco : gerarBlocos(hora, duracaoMinutos)) {
                                    liberarBlocos.put(bloco, null);
                                }
                                mRaiz.child("horarios_ocupados").child(chaveData(data)).updateChildren(liberarBlocos)
                                        .addOnSuccessListener(unused3 ->
                                                listener.onSucesso("Agendamento cancelado com sucesso!", agendamentoId))
                                        .addOnFailureListener(e ->
                                                listener.onErro("Cancelado, mas não consegui liberar o horário: " + e.getMessage()));
                            })
                            .addOnFailureListener(e ->
                                    listener.onErro("Erro ao remover da sua lista pessoal: " + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        listener.onErro("Erro ao cancelar (etapa 1 — lista principal): " + e.getMessage()));
    }

    /** READ — lista TODOS os agendamentos (visão da cabeleireira, lista completa). */
    public void listarTodos(OnAgendamentosListener listener) {
        executarConsulta(mRaiz.child("agendamentos"), listener);
    }

    /** READ — lista os agendamentos de uma cliente específica, lendo só a pasta dela. */
    public void listarPorCliente(String clienteUid, OnAgendamentosListener listener) {
        executarConsulta(mRaiz.child("agendamentos_por_cliente").child(clienteUid), listener);
    }

    private void executarConsulta(Query query, OnAgendamentosListener listener) {
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Agendamento> lista = new ArrayList<>();
                for (DataSnapshot filho : snapshot.getChildren()) {
                    Agendamento agendamento = filho.getValue(Agendamento.class);
                    if (agendamento != null) {
                        agendamento.setId(filho.getKey());
                        lista.add(agendamento);
                    }
                }
                listener.onSucesso(lista);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onErro(error.getMessage());
            }
        });
    }
}
