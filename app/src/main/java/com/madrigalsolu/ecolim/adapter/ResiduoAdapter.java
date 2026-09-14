package com.madrigalsolu.ecolim.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.madrigalsolu.ecolim.R;
import com.madrigalsolu.ecolim.model.Residuo;

import java.util.List;
import java.util.Locale;

public class ResiduoAdapter extends RecyclerView.Adapter<ResiduoAdapter.ResiduoViewHolder> {

    public interface OnResiduoClickListener {
        void onResiduoClick(Residuo residuo);
    }

    private final List<Residuo> listaResiduos;
    private final OnResiduoClickListener listener;

    public ResiduoAdapter(List<Residuo> listaResiduos, OnResiduoClickListener listener) {
        this.listaResiduos = listaResiduos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ResiduoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_residuo, parent, false);
        return new ResiduoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResiduoViewHolder holder, int position) {
        Residuo r = listaResiduos.get(position);
        holder.tvTipo.setText(r.getTipoResiduo());
        holder.tvCantidad.setText(String.format(Locale.getDefault(), "%.2f kg", r.getCantidadKg()));
        holder.tvUbicacion.setText(r.getUbicacion());
        holder.tvFechaHora.setText(r.getFecha() + "  " + r.getHora());
        
        if (r.getSincronizado() == 1) {
            holder.tvEstado.setText("Sincronizado");
            holder.tvEstado.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_synced));
        } else {
            holder.tvEstado.setText("Pendiente");
            holder.tvEstado.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_pending));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onResiduoClick(r);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaResiduos != null ? listaResiduos.size() : 0;
    }

    static class ResiduoViewHolder extends RecyclerView.ViewHolder {
        TextView tvTipo, tvCantidad, tvUbicacion, tvFechaHora, tvEstado;

        ResiduoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTipo = itemView.findViewById(R.id.tv_tipo);
            tvCantidad = itemView.findViewById(R.id.tv_cantidad);
            tvUbicacion = itemView.findViewById(R.id.tv_ubicacion);
            tvFechaHora = itemView.findViewById(R.id.tv_fecha_hora);
            tvEstado = itemView.findViewById(R.id.tv_estado);
        }
    }
}
