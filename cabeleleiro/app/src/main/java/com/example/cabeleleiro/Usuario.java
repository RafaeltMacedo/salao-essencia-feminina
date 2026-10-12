package com.example.cabeleleiro;

/**
 * Representa o perfil do usuário salvo no Firebase Realtime Database,
 * em usuarios/{uid}. Complementa os dados do Firebase Auth (que só guarda
 * e-mail e senha) com nome, telefone e o tipo de conta.
 */
public class Usuario {

    public static final String TIPO_CLIENTE = "cliente";
    public static final String TIPO_CABELEIREIRA = "cabeleireira";

    private String nome;
    private String telefone;
    private String email;
    private String tipo;

    // Construtor vazio é exigido pelo Firebase para reconstruir o objeto
    public Usuario() {
    }

    public Usuario(String nome, String telefone, String email, String tipo) {
        this.nome = nome;
        this.telefone = telefone;
        this.email = email;
        this.tipo = tipo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public boolean isCabeleireira() {
        return TIPO_CABELEIREIRA.equals(tipo);
    }
}
