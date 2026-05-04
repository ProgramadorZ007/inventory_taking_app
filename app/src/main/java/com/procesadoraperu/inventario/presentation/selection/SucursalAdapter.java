package com.procesadoraperu.inventario.presentation.selection;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.domain.model.sucursal.Sucursal;

import java.util.ArrayList;
import java.util.List;

public class SucursalAdapter extends RecyclerView.Adapter<SucursalAdapter.SucursalViewHolder> {

    private List<Sucursal> sucursales = new ArrayList<>();
    private final OnSucursalClickListener listener;

    // Interfaz para manejar el evento clic desde el Activity
    public interface OnSucursalClickListener {
        void onClick(Sucursal sucursal);
    }

    public SucursalAdapter(OnSucursalClickListener listener) {
        this.listener = listener;
    }

    // Actualiza la lista cuando el buscador filtra los datos
    public void setList(List<Sucursal> nuevaLista) {
        this.sucursales = nuevaLista != null ? nuevaLista : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SucursalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflamos el diseño unificado que creaste
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selection, parent, false);
        return new SucursalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SucursalViewHolder holder, int position) {
        Sucursal sucursal = sucursales.get(position);

        // Asignamos los textos
        holder.tvTitle.setText(sucursal.getDescripcion());
        holder.tvSubtitle.setText("Cód. Sucursal: " + sucursal.getIdSucursal());

        // Evento clic de toda la fila (el CardView)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(sucursal);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sucursales.size();
    }

    // ==========================================
    // VIEWHOLDER
    // ==========================================
    static class SucursalViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvSubtitle;
        View viewIconColor; // Opcional: Podrías cambiarle el color dinámicamente luego

        public SucursalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            viewIconColor = itemView.findViewById(R.id.viewIconColor);
        }
    }
}