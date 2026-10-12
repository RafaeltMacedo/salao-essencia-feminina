package com.example.cabeleleiro;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Mostra a lista de serviços e preços pra cliente ver — sem poder editar. */
public class VerServicosActivity extends AppCompatActivity {

    private final ServicoController servicoController = new ServicoController();
    private final List<Servico> lista = new ArrayList<>();
    private ServicoAdapter adapter;
    private TextView txtVazio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_servicos);

        txtVazio = findViewById(R.id.txtVazio);
        RecyclerView recyclerView = findViewById(R.id.recyclerServicos);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ServicoAdapter(lista, null); // listener nulo = modo somente leitura
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                Toast.makeText(VerServicosActivity.this, "Erro ao carregar serviços: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }
}
