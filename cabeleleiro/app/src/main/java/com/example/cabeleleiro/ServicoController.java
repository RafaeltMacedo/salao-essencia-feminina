package com.example.cabeleleiro;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** CRUD dos serviços/preços do salão (servicos/{id}) no Firebase. */
public class ServicoController {

    public interface OnServicosListener {
        void onSucesso(List<Servico> lista);
        void onErro(String erro);
    }

    public interface OnResultadoListener {
        void onSucesso(String mensagem);
        void onErro(String erro);
    }

    private final DatabaseReference mDatabase;

    public ServicoController() {
        mDatabase = FirebaseDatabase.getInstance().getReference().child("servicos");
    }

    public void inserirServico(String nome, double preco, int duracaoMinutos, OnResultadoListener listener) {
        String id = mDatabase.push().getKey();
        if (id == null) {
            listener.onErro("Erro ao gerar código do serviço.");
            return;
        }
        mDatabase.child(id).setValue(new Servico(nome, preco, duracaoMinutos))
                .addOnSuccessListener(unused -> listener.onSucesso("Serviço adicionado com sucesso!"))
                .addOnFailureListener(e -> listener.onErro("Erro ao salvar: " + e.getMessage()));
    }

    public void listarServicos(OnServicosListener listener) {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Servico> lista = new ArrayList<>();
                for (DataSnapshot filho : snapshot.getChildren()) {
                    Servico servico = filho.getValue(Servico.class);
                    if (servico != null) {
                        servico.setId(filho.getKey());
                        lista.add(servico);
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

    public void atualizarServico(String id, String nome, double preco, int duracaoMinutos, OnResultadoListener listener) {
        Map<String, Object> valores = new HashMap<>();
        valores.put("nome", nome);
        valores.put("preco", preco);
        valores.put("duracaoMinutos", duracaoMinutos);
        mDatabase.child(id).updateChildren(valores)
                .addOnSuccessListener(unused -> listener.onSucesso("Serviço atualizado com sucesso!"))
                .addOnFailureListener(e -> listener.onErro("Erro ao atualizar: " + e.getMessage()));
    }

    public void excluirServico(String id, OnResultadoListener listener) {
        mDatabase.child(id).removeValue()
                .addOnSuccessListener(unused -> listener.onSucesso("Serviço removido com sucesso!"))
                .addOnFailureListener(e -> listener.onErro("Erro ao remover: " + e.getMessage()));
    }

    /**
     * Se ainda não existir nenhum serviço cadastrado (primeira vez usando o app),
     * cadastra automaticamente os serviços do salão Essência Feminina — sem
     * preço definido ainda (R$ 0,00), porque a lista original não veio com
     * valores. A cabeleireira ajusta os preços de cada um pela tela
     * "Serviços e Preços" quando quiser.
     */
    public void semearPadraoSeVazio(OnResultadoListener listener) {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    listener.onSucesso("Já existem serviços cadastrados.");
                    return;
                }

                // {nome, duração padrão em minutos} — a cabeleireira ajusta preço e duração depois
                Object[][] servicosPadrao = {
                        {"Corte", 40},
                        {"Escova", 40},
                        {"Cronograma Capilar", 60},
                        {"Redutox", 60},
                        {"Progressiva", 120},
                        {"Terapia Capilar", 60},
                        {"Lisoterapia", 90},
                        {"Coloração", 90},
                        {"Luzes", 120},
                        {"Tratamento Detox", 40},
                        {"Tratamento S.O.S", 40}
                };

                Map<String, Object> padrao = new HashMap<>();
                for (Object[] dados : servicosPadrao) {
                    padrao.put(mDatabase.push().getKey(), new Servico((String) dados[0], 0.00, (int) dados[1]));
                }

                mDatabase.updateChildren(padrao)
                        .addOnSuccessListener(unused -> listener.onSucesso("Serviços padrão criados."))
                        .addOnFailureListener(e -> listener.onErro(e.getMessage()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onErro(error.getMessage());
            }
        });
    }
}
