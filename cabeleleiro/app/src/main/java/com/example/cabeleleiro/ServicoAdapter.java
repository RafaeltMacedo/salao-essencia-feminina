package com.example.cabeleleiro;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class ServicoAdapter extends RecyclerView.Adapter<ServicoAdapter.ServicoViewHolder> {

    public interface OnAcaoListener {
        void onEditar(Servico servico);
        void onExcluir(Servico servico);
    }

    private final List<Servico> lista;
    private final OnAcaoListener listener;

    public ServicoAdapter(List<Servico> lista, OnAcaoListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ServicoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_servico, parent, false);
        return new ServicoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServicoViewHolder holder, int position) {
        Servico servico = lista.get(position);
        holder.txtNome.setText(servico.getNome());
        holder.txtPreco.setText("R$ " + String.format(Locale.getDefault(), "%.2f", servico.getPreco()).replace(".", ",")
                + "  •  " + servico.getDuracaoMinutos() + " min");

        if (listener != null) {
            holder.btEditar.setVisibility(View.VISIBLE);
            holder.btExcluir.setVisibility(View.VISIBLE);
            holder.btEditar.setOnClickListener(v -> listener.onEditar(servico));
            holder.btExcluir.setOnClickListener(v -> listener.onExcluir(servico));
        } else {
            // Modo somente leitura (usado na tela da cliente) — sem botões de ação
            holder.btEditar.setVisibility(View.GONE);
            holder.btExcluir.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ServicoViewHolder extends RecyclerView.ViewHolder {
        TextView txtNome, txtPreco;
        Button btEditar, btExcluir;

        ServicoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNome = itemView.findViewById(R.id.txtNomeServico);
            txtPreco = itemView.findViewById(R.id.txtPrecoServico);
            btEditar = itemView.findViewById(R.id.btEditarServico);
            btExcluir = itemView.findViewById(R.id.btExcluirServico);
        }
    }
}
