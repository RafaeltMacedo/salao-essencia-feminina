package com.example.cabeleleiro;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controla quais dias da semana o salão funciona, salvo em
 * configuracoes/diasFuncionamento. Editável pela cabeleireira na tela
 * "Dias de Funcionamento". Por padrão (se nunca foi configurado), todos
 * os dias contam como abertos.
 */
public class ConfiguracaoController {

    public interface OnDiasListener {
        void onSucesso(Map<String, Boolean> dias);
        void onErro(String erro);
    }

    public interface OnResultadoListener {
        void onSucesso();
        void onErro(String erro);
    }

    /** Chaves na mesma ordem de Calendar.DAY_OF_WEEK (1=domingo ... 7=sábado). */
    public static final String[] CHAVES_DIAS = {
            "domingo", "segunda", "terca", "quarta", "quinta", "sexta", "sabado"
    };

    public static final String[] NOMES_EXIBICAO = {
            "Domingo", "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado"
    };

    private final DatabaseReference mDatabase;

    public ConfiguracaoController() {
        mDatabase = FirebaseDatabase.getInstance().getReference().child("configuracoes").child("diasFuncionamento");
    }

    public void buscarDias(OnDiasListener listener) {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Boolean> resultado = new LinkedHashMap<>();
                for (String chave : CHAVES_DIAS) {
                    Boolean valor = snapshot.child(chave).getValue(Boolean.class);
                    resultado.put(chave, valor == null || valor); // padrão: aberto
                }
                listener.onSucesso(resultado);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onErro(error.getMessage());
            }
        });
    }

    public void salvarDias(Map<String, Boolean> dias, OnResultadoListener listener) {
        mDatabase.setValue(dias)
                .addOnSuccessListener(unused -> listener.onSucesso())
                .addOnFailureListener(e -> listener.onErro(e.getMessage()));
    }
}
