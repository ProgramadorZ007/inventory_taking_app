package com.procesadoraperu.inventario.presentation.selection;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.domain.model.almacen.Almacen;

import java.util.ArrayList;
import java.util.List;

public class AlmacenAdapter extends RecyclerView.Adapter<AlmacenAdapter.AlmacenViewHolder> {

    private List<Almacen> almacenes = new ArrayList<>();
    private final OnAlmacenClickListener listener;

    public interface OnAlmacenClickListener {
        void onClick(Almacen almacen);
    }

    public AlmacenAdapter(OnAlmacenClickListener listener) {
        this.listener = listener;
    }

    public void setList(List<Almacen> nuevaLista) {
        this.almacenes = nuevaLista != null ? nuevaLista : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlmacenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selection, parent, false);
        return new AlmacenViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlmacenViewHolder holder, int position) {
        Almacen almacen = almacenes.get(position);

        holder.tvTitle.setText(almacen.getDescripcion());
        holder.tvSubtitle.setText("Cód. Almacén: " + almacen.getIdAlmacen());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(almacen);
            }
        });
    }

    @Override
    public int getItemCount() {
        return almacenes.size();
    }

    static class AlmacenViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvSubtitle;

        public AlmacenViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
        }
    }
}