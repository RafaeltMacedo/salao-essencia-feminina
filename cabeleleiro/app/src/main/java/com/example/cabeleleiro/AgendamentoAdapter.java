package com.example.cabeleleiro;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AgendamentoAdapter extends RecyclerView.Adapter<AgendamentoAdapter.AgendamentoViewHolder> {

    public interface OnAcaoListener {
        void onEditar(Agendamento agendamento);
        void onExcluir(Agendamento agendamento);
        void onConcluir(Agendamento agendamento);
        void onEnviarWhatsapp(Agendamento agendamento);
    }

    private final List<Agendamento> lista;
    private final boolean mostrarNomeCliente; // true para a cabeleireira (vê o nome de cada cliente)
    private final boolean mostrarAcaoConcluir; // true só para a cabeleireira
    private final OnAcaoListener listener;

    public AgendamentoAdapter(List<Agendamento> lista, boolean mostrarNomeCliente,
                               boolean mostrarAcaoConcluir, OnAcaoListener listener) {
        this.lista = lista;
        this.mostrarNomeCliente = mostrarNomeCliente;
        this.mostrarAcaoConcluir = mostrarAcaoConcluir;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AgendamentoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_agendamento, parent, false);
        return new AgendamentoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AgendamentoViewHolder holder, int position) {
        Agendamento agendamento = lista.get(position);

        if (mostrarNomeCliente) {
            holder.txtNome.setVisibility(View.VISIBLE);
            holder.txtNome.setText(agendamento.getNomeCliente());
        } else {
            holder.txtNome.setVisibility(View.GONE);
        }

        holder.txtServico.setText(agendamento.getServico());
        holder.txtDataHora.setText(agendamento.getData() + " às " + agendamento.getHora()
                + "  (" + agendamento.getDuracaoMinutos() + " min)");

        boolean concluido = Agendamento.STATUS_CONCLUIDO.equals(agendamento.getStatus());
        holder.txtStatus.setText(concluido ? "Concluído" : "Pendente");
        holder.txtStatus.setTextColor(concluido ? 0xFF43A047 : 0xFFC9A227);

        holder.btEditar.setOnClickListener(v -> listener.onEditar(agendamento));
        holder.btExcluir.setOnClickListener(v -> listener.onExcluir(agendamento));

        if (mostrarAcaoConcluir && !concluido) {
            holder.btConcluir.setVisibility(View.VISIBLE);
            holder.btConcluir.setOnClickListener(v -> listener.onConcluir(agendamento));
        } else {
            holder.btConcluir.setVisibility(View.GONE);
        }

        boolean temTelefone = agendamento.getTelefoneCliente() != null && !agendamento.getTelefoneCliente().trim().isEmpty();
        if (mostrarNomeCliente && temTelefone) {
            holder.btWhatsapp.setVisibility(View.VISIBLE);
            holder.btWhatsapp.setOnClickListener(v -> listener.onEnviarWhatsapp(agendamento));
        } else {
            holder.btWhatsapp.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class AgendamentoViewHolder extends RecyclerView.ViewHolder {
        TextView txtNome, txtServico, txtDataHora, txtStatus;
        Button btEditar, btExcluir, btConcluir, btWhatsapp;

        AgendamentoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNome = itemView.findViewById(R.id.txtNome);
            txtServico = itemView.findViewById(R.id.txtServico);
            txtDataHora = itemView.findViewById(R.id.txtDataHora);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            btEditar = itemView.findViewById(R.id.btEditar);
            btExcluir = itemView.findViewById(R.id.btExcluir);
            btConcluir = itemView.findViewById(R.id.btConcluir);
            btWhatsapp = itemView.findViewById(R.id.btWhatsapp);
        }
    }
}
