package com.procesadoraperu.inventario.presentation.inventory.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.domain.model.inventario.Inventario;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InventoryHistoryAdapter extends RecyclerView.Adapter<InventoryHistoryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(Inventario inv);
    }

    private final List<Inventario> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public InventoryHistoryAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setList(List<Inventario> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Inventario inv = items.get(position);

        holder.tvProducto.setText(inv.getProducto() != null ? inv.getProducto().toUpperCase() : "—");
        holder.tvCodigo.setText("Código: " + (inv.getIdProducto() != null ? inv.getIdProducto() : "—"));

        String unidad = inv.getUnidadMedida() != null ? inv.getUnidadMedida() : "UND";
        holder.tvCantidadContada.setText(formatNum(inv.getCantidad()) + " " + unidad);
        holder.tvStockSistema.setText(formatNum(inv.getStock()) + " " + unidad);

        // Fecha y Hora
        String fechaRaw = inv.getFechaCreacion() != null
                ? inv.getFechaCreacion() : inv.getFechaRegistroLocal();
        
        if (fechaRaw != null) {
            try {
                // Limpieza básica si viene con T
                String cleanedDate = fechaRaw.replace("T", " ");
                if (cleanedDate.contains(".")) {
                    cleanedDate = cleanedDate.substring(0, cleanedDate.lastIndexOf("."));
                }
                
                SimpleDateFormat sdfSource = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = sdfSource.parse(cleanedDate);
                
                if (date != null) {
                    holder.tvFecha.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date));
                    holder.tvHora.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date).toUpperCase());
                } else {
                    holder.tvFecha.setText(fechaRaw);
                    holder.tvHora.setText("");
                }
            } catch (Exception e) {
                holder.tvFecha.setText(fechaRaw);
                holder.tvHora.setText("");
            }
        } else {
            holder.tvFecha.setText("—");
            holder.tvHora.setText("");
        }

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
        TextView tvProducto, tvCodigo, tvCantidadContada, tvStockSistema, tvFecha, tvHora;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProducto        = itemView.findViewById(R.id.tvProducto);
            tvCodigo          = itemView.findViewById(R.id.tvCodigo);
            tvCantidadContada = itemView.findViewById(R.id.tvCantidadContada);
            tvStockSistema    = itemView.findViewById(R.id.tvStockSistema);
            tvFecha           = itemView.findViewById(R.id.tvFecha);
            tvHora            = itemView.findViewById(R.id.tvHora);
        }
    }
}