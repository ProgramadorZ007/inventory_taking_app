package com.procesadoraperu.inventario.presentation.inventory.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.domain.model.inventario.Inventario;

import java.util.ArrayList;
import java.util.List;

public class InventoryHistoryAdapter
        extends RecyclerView.Adapter<InventoryHistoryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(Inventario inventario);
    }

    private List<Inventario> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public InventoryHistoryAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setList(List<Inventario> list) {
        this.items = (list != null) ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Inventario inv = items.get(position);

        holder.tvProducto.setText(inv.getProducto() != null ? inv.getProducto() : "—");
        holder.tvCodigo.setText("Cód: " + (inv.getIdProducto() != null ? inv.getIdProducto() : "—"));

        String unidad = inv.getUnidadMedida() != null ? inv.getUnidadMedida() : "";
        holder.tvCantidad.setText("Contado: " + formatNum(inv.getCantidad()) + " " + unidad);
        holder.tvStock.setText("Sistema: " + formatNum(inv.getStock()));

        // Fecha
        String fecha = inv.getFechaCreacion() != null
                ? inv.getFechaCreacion() : inv.getFechaRegistroLocal();
        if (fecha != null && fecha.contains("T")) {
            fecha = fecha.replace("T", "  ");
            if (fecha.length() > 19) fecha = fecha.substring(0, 19);
        }
        holder.tvFecha.setText(fecha != null ? fecha : "—");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(inv);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String formatNum(double val) {
        if (val == Math.floor(val) && !Double.isInfinite(val)) {
            return String.valueOf((int) val);
        }
        return String.valueOf(val);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProducto, tvCodigo, tvCantidad, tvStock, tvFecha;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProducto = itemView.findViewById(R.id.tvProducto);
            tvCodigo   = itemView.findViewById(R.id.tvCodigo);
            tvCantidad = itemView.findViewById(R.id.tvCantidad);
            tvStock    = itemView.findViewById(R.id.tvStock);
            tvFecha    = itemView.findViewById(R.id.tvFecha);
        }
    }
}