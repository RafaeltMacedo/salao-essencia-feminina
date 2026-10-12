package com.example.cabeleleiro;

/**
 * Representa um agendamento salvo no Firebase Realtime Database,
 * em agendamentos/{id}.
 */
public class Agendamento {

    public static final String STATUS_PENDENTE = "pendente";
    public static final String STATUS_CONCLUIDO = "concluido";

    // Não é salvo dentro do próprio nó — é a chave (push key) do Firebase,
    // preenchida depois de ler os dados.
    private String id;

    private String clienteUid;
    private String nomeCliente;
    private String telefoneCliente;
    private String servico;
    private String data;
    private String hora;
    private String status;
    private long criadoEm; // timestamp de quando foi criado, usado pelo serviço de aviso em tempo real
    private int duracaoMinutos = 40; // duração do serviço NO MOMENTO do agendamento (não muda se o serviço for editado depois)

    // Construtor vazio é exigido pelo Firebase para reconstruir o objeto
    public Agendamento() {
    }

    public Agendamento(String clienteUid, String nomeCliente, String telefoneCliente,
                        String servico, String data, String hora, String status) {
        this.clienteUid = clienteUid;
        this.nomeCliente = nomeCliente;
        this.telefoneCliente = telefoneCliente;
        this.servico = servico;
        this.data = data;
        this.hora = hora;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClienteUid() {
        return clienteUid;
    }

    public void setClienteUid(String clienteUid) {
        this.clienteUid = clienteUid;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    public String getTelefoneCliente() {
        return telefoneCliente;
    }

    public void setTelefoneCliente(String telefoneCliente) {
        this.telefoneCliente = telefoneCliente;
    }

    public String getServico() {
        return servico;
    }

    public void setServico(String servico) {
        this.servico = servico;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(long criadoEm) {
        this.criadoEm = criadoEm;
    }

    public int getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public void setDuracaoMinutos(int duracaoMinutos) {
        this.duracaoMinutos = duracaoMinutos;
    }
}
