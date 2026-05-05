package com.procesadoraperu.inventario.presentation.inventory.pending;

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

public class PendingInventoryAdapter
        extends RecyclerView.Adapter<PendingInventoryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(Inventario inventario);
    }

    private List<Inventario> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public PendingInventoryAdapter(OnItemClickListener listener) {
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
                .inflate(R.layout.item_pending, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Inventario inv = items.get(position);

        holder.tvProducto.setText(inv.getProducto() != null ? inv.getProducto() : "—");

        String unidad = inv.getUnidadMedida() != null ? inv.getUnidadMedida() : "";
        holder.tvCantidad.setText("Cantidad: " + formatNum(inv.getCantidad()) + " " + unidad);
        holder.tvAlmacen.setText(inv.getAlmacen() != null ? inv.getAlmacen() : "—");

        String fecha = inv.getFechaRegistroLocal() != null
                ? inv.getFechaRegistroLocal() : "—";
        holder.tvFecha.setText(fecha);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(inv);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String formatNum(double val) {
        if (val == Math.floor(val) && !Double.isInfinite(val)) return String.valueOf((int) val);
        return String.valueOf(val);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProducto, tvCantidad, tvAlmacen, tvFecha;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProducto = itemView.findViewById(R.id.tvProducto);
            tvCantidad = itemView.findViewById(R.id.tvCantidad);
            tvAlmacen  = itemView.findViewById(R.id.tvAlmacen);
            tvFecha    = itemView.findViewById(R.id.tvFecha);
        }
    }
}