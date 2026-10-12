package com.example.cabeleleiro;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/** Tela simples mostrando a profissional responsável pelo salão e suas especialidades. */
public class ProfissionaisActivity extends AppCompatActivity {

    private final ServicoController servicoController = new ServicoController();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profissionais);

        LinearLayout container = findViewById(R.id.containerEspecialidades);

        servicoController.listarServicos(new ServicoController.OnServicosListener() {
            @Override
            public void onSucesso(List<Servico> lista) {
                if (lista.isEmpty()) {
                    adicionarLinha(container, "Especialidades ainda não cadastradas.");
                    return;
                }
                for (Servico servico : lista) {
                    adicionarLinha(container, "• " + servico.getNome());
                }
            }

            @Override
            public void onErro(String erro) {
                adicionarLinha(container, "Não foi possível carregar as especialidades.");
            }
        });
    }

    private void adicionarLinha(LinearLayout container, String texto) {
        TextView textView = new TextView(this);
        textView.setText(texto);
        textView.setTextColor(getResources().getColor(R.color.cor_texto_claro));
        textView.setTextSize(15);
        textView.setPadding(0, 6, 0, 6);
        container.addView(textView);
    }
}
