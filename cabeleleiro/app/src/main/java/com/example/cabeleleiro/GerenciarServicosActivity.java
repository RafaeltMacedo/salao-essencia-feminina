package com.example.cabeleleiro;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tela onde a cabeleireira cadastra, edita e remove os serviços e preços
 * do salão — sem precisar mexer no código no Android Studio.
 */
public class GerenciarServicosActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView txtVazio;
    private FloatingActionButton fabNovo;

    private final ServicoController servicoController = new ServicoController();
    private final List<Servico> lista = new ArrayList<>();
    private ServicoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gerenciar_servicos);

        txtVazio = findViewById(R.id.txtVazio);
        recyclerView = findViewById(R.id.recyclerServicos);
        fabNovo = findViewById(R.id.fabNovo);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ServicoAdapter(lista, new ServicoAdapter.OnAcaoListener() {
            @Override
            public void onEditar(Servico servico) {
                abrirDialogServico(servico);
            }

            @Override
            public void onExcluir(Servico servico) {
                confirmarExclusao(servico);
            }
        });
        recyclerView.setAdapter(adapter);

        fabNovo.setOnClickListener(v -> abrirDialogServico(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarLista();
    }

    private void carregarLista() {
        servicoController.listarServicos(new ServicoController.OnServicosListener() {
            @Override
            public void onSucesso(List<Servico> novaLista) {
                lista.clear();
                lista.addAll(novaLista);
                adapter.notifyDataSetChanged();
                txtVazio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(GerenciarServicosActivity.this, "Erro ao carregar serviços: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Abre o diálogo de cadastro (servico == null) ou edição (servico preenchido). */
    private void abrirDialogServico(Servico servico) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_servico, null);
        EditText txtNome = view.findViewById(R.id.txtNomeDialog);
        EditText txtPreco = view.findViewById(R.id.txtPrecoDialog);
        EditText txtDuracao = view.findViewById(R.id.txtDuracaoDialog);

        boolean editando = servico != null;
        if (editando) {
            txtNome.setText(servico.getNome());
            txtPreco.setText(String.format(Locale.getDefault(), "%.2f", servico.getPreco()).replace(".", ","));
            txtDuracao.setText(String.valueOf(servico.getDuracaoMinutos()));
        } else {
            txtDuracao.setText("40");
        }

        new AlertDialog.Builder(this)
                .setTitle(editando ? "Editar serviço" : "Novo serviço")
                .setView(view)
                .setPositiveButton(editando ? "Salvar" : "Adicionar", (dialog, which) -> {
                    String nome = txtNome.getText().toString().trim();
                    String precoTexto = txtPreco.getText().toString().trim().replace(",", ".");
                    String duracaoTexto = txtDuracao.getText().toString().trim();

                    if (nome.isEmpty() || precoTexto.isEmpty() || duracaoTexto.isEmpty()) {
                        Toast.makeText(this, "Preencha nome, preço e duração!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double preco;
                    try {
                        preco = Double.parseDouble(precoTexto);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Preço inválido. Use, por exemplo, 25,00", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int duracao;
                    try {
                        duracao = Integer.parseInt(duracaoTexto);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Duração inválida. Use um número de minutos, ex: 40", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (duracao <= 0) {
                        Toast.makeText(this, "A duração precisa ser maior que zero.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ServicoController.OnResultadoListener listener = new ServicoController.OnResultadoListener() {
                        @Override
                        public void onSucesso(String mensagem) {
                            Toast.makeText(GerenciarServicosActivity.this, mensagem, Toast.LENGTH_SHORT).show();
                            carregarLista();
                        }

                        @Override
                        public void onErro(String erro) {
                            Toast.makeText(GerenciarServicosActivity.this, erro, Toast.LENGTH_LONG).show();
                        }
                    };

                    if (editando) {
                        servicoController.atualizarServico(servico.getId(), nome, preco, duracao, listener);
                    } else {
                        servicoController.inserirServico(nome, preco, duracao, listener);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarExclusao(Servico servico) {
        new AlertDialog.Builder(this)
                .setTitle("Remover serviço")
                .setMessage("Tem certeza que deseja remover \"" + servico.getNome() + "\"?")
                .setPositiveButton("Remover", (dialog, which) ->
                        servicoController.excluirServico(servico.getId(), new ServicoController.OnResultadoListener() {
                            @Override
                            public void onSucesso(String mensagem) {
                                Toast.makeText(GerenciarServicosActivity.this, mensagem, Toast.LENGTH_SHORT).show();
                                carregarLista();
                            }

                            @Override
                            public void onErro(String erro) {
                                Toast.makeText(GerenciarServicosActivity.this, erro, Toast.LENGTH_LONG).show();
                            }
                        }))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
