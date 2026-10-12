package com.example.cabeleleiro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Mostra os agendamentos. Em "modo cliente" mostra só os da própria cliente.
 * Em "modo cabeleireira" mostra TODOS os agendamentos do salão, com opção
 * de marcar como concluído.
 */
public class ListaAgendamentosActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView txtVazio, txtTitulo;
    private FloatingActionButton fabNovo;

    private final BancoController bancoController = new BancoController(this);
    private final List<Agendamento> lista = new ArrayList<>();
    private AgendamentoAdapter adapter;

    private boolean modoCabeleireira;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_agendamentos);

        txtTitulo = findViewById(R.id.txtTitulo);
        txtVazio = findViewById(R.id.txtVazio);
        recyclerView = findViewById(R.id.recyclerAgendamentos);
        fabNovo = findViewById(R.id.fabNovo);

        modoCabeleireira = "cabeleireira".equals(getIntent().getStringExtra("modo"));
        txtTitulo.setText(modoCabeleireira ? "Agenda do Salão" : "Meus Agendamentos");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AgendamentoAdapter(lista, modoCabeleireira, modoCabeleireira, new AgendamentoAdapter.OnAcaoListener() {
            @Override
            public void onEditar(Agendamento agendamento) {
                Intent tela = new Intent(ListaAgendamentosActivity.this, AgendarActivity.class);
                tela.putExtra("agendamentoId", agendamento.getId());
                tela.putExtra("clienteUid", agendamento.getClienteUid());
                tela.putExtra("nomeCliente", agendamento.getNomeCliente());
                tela.putExtra("telefoneCliente", agendamento.getTelefoneCliente());
                tela.putExtra("servico", agendamento.getServico());
                tela.putExtra("data", agendamento.getData());
                tela.putExtra("hora", agendamento.getHora());
                tela.putExtra("duracaoMinutos", agendamento.getDuracaoMinutos());
                startActivity(tela);
            }

            @Override
            public void onExcluir(Agendamento agendamento) {
                confirmarExclusao(agendamento);
            }

            @Override
            public void onConcluir(Agendamento agendamento) {
                bancoController.marcarConcluido(agendamento.getId(), agendamento.getClienteUid(), new BancoController.OnResultadoListener() {
                    @Override
                    public void onSucesso(String mensagem, String idAgendamento) {
                        Toast.makeText(ListaAgendamentosActivity.this, mensagem, Toast.LENGTH_SHORT).show();
                        carregarLista();
                    }

                    @Override
                    public void onErro(String erro) {
                        Toast.makeText(ListaAgendamentosActivity.this, erro, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onEnviarWhatsapp(Agendamento agendamento) {
                abrirWhatsapp(agendamento);
            }
        });
        recyclerView.setAdapter(adapter);

        fabNovo.setOnClickListener(v -> startActivity(new Intent(this, AgendarActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarLista(); // recarrega sempre que volta pra essa tela (depois de editar/excluir/criar)
    }

    private void carregarLista() {
        BancoController.OnAgendamentosListener listener = new BancoController.OnAgendamentosListener() {
            @Override
            public void onSucesso(List<Agendamento> novaLista) {
                lista.clear();
                lista.addAll(novaLista);
                adapter.notifyDataSetChanged();
                txtVazio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(ListaAgendamentosActivity.this, "Erro ao carregar agendamentos: " + erro, Toast.LENGTH_LONG).show();
            }
        };

        if (modoCabeleireira) {
            bancoController.listarTodos(listener);
        } else {
            FirebaseUser usuarioAtual = FirebaseAuth.getInstance().getCurrentUser();
            if (usuarioAtual != null) {
                bancoController.listarPorCliente(usuarioAtual.getUid(), listener);
            }
        }
    }

    /** Abre o WhatsApp já com o número da cliente e uma mensagem de confirmação pronta. */
    private void abrirWhatsapp(Agendamento agendamento) {
        String telefone = agendamento.getTelefoneCliente();
        if (telefone == null || telefone.trim().isEmpty()) {
            Toast.makeText(this, "Essa cliente não tem telefone cadastrado.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Deixa só os números e garante o código do Brasil (55) na frente
        String apenasNumeros = telefone.replaceAll("[^0-9]", "");
        if (!apenasNumeros.startsWith("55")) {
            apenasNumeros = "55" + apenasNumeros;
        }

        String mensagem = "Olá " + agendamento.getNomeCliente() + "! Confirmando seu horário dia "
                + agendamento.getData() + " às " + agendamento.getHora()
                + " (" + agendamento.getServico() + ") na Essência Feminina. Até lá! 💇‍♀️";

        String url;
        try {
            String mensagemCodificada = java.net.URLEncoder.encode(mensagem, "UTF-8");
            url = "https://wa.me/" + apenasNumeros + "?text=" + mensagemCodificada;
        } catch (java.io.UnsupportedEncodingException e) {
            url = "https://wa.me/" + apenasNumeros;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir o WhatsApp. Verifique se ele está instalado.", Toast.LENGTH_LONG).show();
        }
    }

    private void confirmarExclusao(Agendamento agendamento) {
        new AlertDialog.Builder(this)
                .setTitle("Cancelar agendamento")
                .setMessage("Tem certeza que deseja cancelar o agendamento de " + agendamento.getNomeCliente() + "?")
                .setPositiveButton("Sim, cancelar", (dialog, which) ->
                        bancoController.excluirDados(agendamento.getId(), agendamento.getData(), agendamento.getHora(),
                                agendamento.getDuracaoMinutos(), agendamento.getClienteUid(), new BancoController.OnResultadoListener() {
                            @Override
                            public void onSucesso(String mensagem, String idAgendamento) {
                                Toast.makeText(ListaAgendamentosActivity.this, mensagem, Toast.LENGTH_SHORT).show();
                                NotificationHelper.cancelarLembrete(ListaAgendamentosActivity.this, idAgendamento);
                                carregarLista();
                            }

                            @Override
                            public void onErro(String erro) {
                                Toast.makeText(ListaAgendamentosActivity.this, erro, Toast.LENGTH_LONG).show();
                            }
                        }))
                .setNegativeButton("Voltar", null)
                .show();
    }
}
