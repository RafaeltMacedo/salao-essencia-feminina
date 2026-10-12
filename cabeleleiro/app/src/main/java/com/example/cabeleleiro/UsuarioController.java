package com.example.cabeleleiro;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Cuida do perfil do usuário (nome, telefone, tipo) no Firebase Realtime
 * Database. Isso é separado do Firebase Auth porque o Auth só guarda
 * e-mail/senha — os outros dados (e o tipo: cliente ou cabeleireira)
 * precisam ficar em outro lugar.
 */
public class UsuarioController {

    public interface OnUsuarioListener {
        void onSucesso(Usuario usuario);
        void onErro(String erro);
    }

    public interface OnResultadoListener {
        void onSucesso();
        void onErro(String erro);
    }

    private final DatabaseReference mDatabase;

    public UsuarioController() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    /** Salva o perfil do usuário recém-cadastrado em usuarios/{uid}. */
    public void salvarPerfil(String uid, Usuario usuario, OnResultadoListener listener) {
        mDatabase.child("usuarios").child(uid).setValue(usuario)
                .addOnSuccessListener(unused -> listener.onSucesso())
                .addOnFailureListener(e -> listener.onErro(e.getMessage()));
    }

    /** Busca o perfil (e o tipo: cliente ou cabeleireira) de um usuário logado. */
    public void buscarPerfil(String uid, OnUsuarioListener listener) {
        mDatabase.child("usuarios").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Usuario usuario = snapshot.getValue(Usuario.class);
                if (usuario != null) {
                    listener.onSucesso(usuario);
                } else {
                    listener.onErro("Perfil de usuário não encontrado.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onErro(error.getMessage());
            }
        });
    }

    /** Atualiza só nome e telefone (mantém tipo e e-mail como estavam). Usado na tela de Perfil. */
    public void atualizarNomeTelefone(String uid, String nome, String telefone, OnResultadoListener listener) {
        java.util.Map<String, Object> valores = new java.util.HashMap<>();
        valores.put("nome", nome);
        valores.put("telefone", telefone);
        mDatabase.child("usuarios").child(uid).updateChildren(valores)
                .addOnSuccessListener(unused -> listener.onSucesso())
                .addOnFailureListener(e -> listener.onErro(e.getMessage()));
    }
}
