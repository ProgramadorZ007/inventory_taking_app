package com.procesadoraperu.inventario.presentation.inventory.history;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.domain.model.inventario.Inventario;
import com.procesadoraperu.inventario.presentation.ViewModelFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class InventoryHistoryActivity extends AppCompatActivity {

    private InventoryHistoryViewModel viewModel;
    private InventoryHistoryAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_history);

        // Forzamos que los iconos de la barra de estado sean blancos sobre el fondo verde oscuro
        if (getWindow() != null) {
            new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                    .setAppearanceLightStatusBars(false);
        }

        // Manejo de Insets para que el color verde suba pero el texto no se mezcle con la hora/batería
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.history_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.appbar).setPadding(0, systemBars.top, 0, 0);
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        tvEmpty     = findViewById(R.id.tvEmpty);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryHistoryAdapter(this::mostrarDetalle);
        recyclerView.setAdapter(adapter);

        ViewModelFactory factory = new ViewModelFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(InventoryHistoryViewModel.class);

        viewModel.getIsLoading().observe(this, loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getHistorial().observe(this, lista -> {
            adapter.setList(lista);
            tvEmpty.setVisibility(
                    (lista == null || lista.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });

        // Llamamos al método que carga los últimos 3 meses
        viewModel.cargarHistorialUltimos3Meses();
    }

    private void mostrarDetalle(Inventario inv) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_inventory_detail, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Poblar datos
        ((TextView) dialogView.findViewById(R.id.tvDialogSubtitle)).setText(inv.getProducto());
        ((TextView) dialogView.findViewById(R.id.tvDetailCodigo)).setText(inv.getIdProducto());
        
        String unidad = (inv.getUnidadMedida() != null) ? inv.getUnidadMedida() : "UND";
        ((TextView) dialogView.findViewById(R.id.tvDetailUnidad)).setText(unidad);
        ((TextView) dialogView.findViewById(R.id.tvDetailUnidadStock)).setText(unidad);
        ((TextView) dialogView.findViewById(R.id.tvDetailUnidadContado)).setText(unidad);

        ((TextView) dialogView.findViewById(R.id.tvDetailStockSistema)).setText(formatNum(inv.getStock()));
        ((TextView) dialogView.findViewById(R.id.tvDetailCantidadContada)).setText(formatNum(inv.getCantidad()));

        ((TextView) dialogView.findViewById(R.id.tvDetailAlmacen)).setText(inv.getAlmacen());
        ((TextView) dialogView.findViewById(R.id.tvDetailSucursal)).setText(inv.getSucursal());
        ((TextView) dialogView.findViewById(R.id.tvDetailUser)).setText(inv.getUsuarioCreacion());

        // Formateo de Fecha y Hora
        String fechaRaw = inv.getFechaCreacion() != null ? inv.getFechaCreacion() : inv.getFechaRegistroLocal();
        if (fechaRaw != null) {
            try {
                String cleanedDate = fechaRaw.replace("T", " ");
                if (cleanedDate.contains(".")) cleanedDate = cleanedDate.substring(0, cleanedDate.lastIndexOf("."));
                SimpleDateFormat sdfSource = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = sdfSource.parse(cleanedDate);
                if (date != null) {
                    String formatted = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date) + " • " +
                                     new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date).toUpperCase();
                    ((TextView) dialogView.findViewById(R.id.tvDetailDateTime)).setText(formatted);
                }
            } catch (Exception e) {
                ((TextView) dialogView.findViewById(R.id.tvDetailDateTime)).setText(fechaRaw);
            }
        }

        dialogView.findViewById(R.id.btnDetailCerrar).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String formatNum(double val) {
        if (val == Math.floor(val) && !Double.isInfinite(val)) return String.valueOf((int) val);
        return String.valueOf(val);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}