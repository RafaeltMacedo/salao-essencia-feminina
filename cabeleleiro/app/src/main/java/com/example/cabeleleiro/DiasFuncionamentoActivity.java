package com.example.cabeleleiro;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.LinkedHashMap;
import java.util.Map;

public class DiasFuncionamentoActivity extends AppCompatActivity {

    private final ConfiguracaoController configuracaoController = new ConfiguracaoController();
    private final Map<String, Switch> switchesPorDia = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dias_funcionamento);

        LinearLayout container = findViewById(R.id.containerDias);
        Button btSalvar = findViewById(R.id.btSalvar);

        for (int i = 0; i < ConfiguracaoController.CHAVES_DIAS.length; i++) {
            String chave = ConfiguracaoController.CHAVES_DIAS[i];
            String nomeExibido = ConfiguracaoController.NOMES_EXIBICAO[i];

            View linha = LayoutInflater.from(this).inflate(R.layout.item_dia_funcionamento, container, false);
            ((TextView) linha.findViewById(R.id.txtNomeDia)).setText(nomeExibido);
            Switch interruptor = linha.findViewById(R.id.switchDia);

            switchesPorDia.put(chave, interruptor);
            container.addView(linha);
        }

        carregarConfiguracaoAtual();

        btSalvar.setOnClickListener(v -> salvar());
    }

    private void carregarConfiguracaoAtual() {
        configuracaoController.buscarDias(new ConfiguracaoController.OnDiasListener() {
            @Override
            public void onSucesso(Map<String, Boolean> dias) {
                for (Map.Entry<String, Boolean> entrada : dias.entrySet()) {
                    Switch interruptor = switchesPorDia.get(entrada.getKey());
                    if (interruptor != null) {
                        interruptor.setChecked(entrada.getValue());
                    }
                }
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(DiasFuncionamentoActivity.this, "Erro ao carregar: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void salvar() {
        Map<String, Boolean> dias = new LinkedHashMap<>();
        for (Map.Entry<String, Switch> entrada : switchesPorDia.entrySet()) {
            dias.put(entrada.getKey(), entrada.getValue().isChecked());
        }

        configuracaoController.salvarDias(dias, new ConfiguracaoController.OnResultadoListener() {
            @Override
            public void onSucesso() {
                Toast.makeText(DiasFuncionamentoActivity.this, "Dias de funcionamento atualizados!", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(DiasFuncionamentoActivity.this, "Erro ao salvar: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }
}
