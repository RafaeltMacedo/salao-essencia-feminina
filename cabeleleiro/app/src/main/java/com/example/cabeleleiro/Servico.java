package com.example.cabeleleiro;

import java.util.Locale;

/**
 * Representa um serviço do salão (nome + preço) salvo em servicos/{id}.
 * Antes isso era fixo no código (strings.xml) — agora a cabeleireira
 * cadastra e edita pelo próprio app.
 */
public class Servico {

    private String id;
    private String nome;
    private double preco;
    private int duracaoMinutos = 40; // padrão, caso não seja definido

    // Construtor vazio é exigido pelo Firebase
    public Servico() {
    }

    public Servico(String nome, double preco) {
        this.nome = nome;
        this.preco = preco;
    }

    public Servico(String nome, double preco, int duracaoMinutos) {
        this.nome = nome;
        this.preco = preco;
        this.duracaoMinutos = duracaoMinutos;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public double getPreco() {
        return preco;
    }

    public void setPreco(double preco) {
        this.preco = preco;
    }

    public int getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public void setDuracaoMinutos(int duracaoMinutos) {
        this.duracaoMinutos = duracaoMinutos;
    }

    /** Texto pronto pra exibir, ex: "Corte Feminino — R$ 16,00" */
    public String getTextoExibicao() {
        return nome + " — R$ " + String.format(Locale.getDefault(), "%.2f", preco).replace(".", ",");
    }
}
