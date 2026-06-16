package com.procesadoraperu.inventario.presentation.inventory.pending;

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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.domain.model.inventario.Inventario;
import com.procesadoraperu.inventario.presentation.ViewModelFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PendingInventoryActivity extends AppCompatActivity {

    private PendingInventoryViewModel viewModel;
    private PendingInventoryAdapter adapter;

    private ProgressBar progressBar, progressSync;
    private TextView tvEmpty, tvContador;
    private MaterialButton btnSincronizar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_inventory);

        // Forzamos que los iconos de la barra de estado sean blancos
        if (getWindow() != null) {
            new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                    .setAppearanceLightStatusBars(false);
        }

        // Manejo de Insets para pantalla completa
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pending_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.appbar).setPadding(0, systemBars.top, 0, 0);
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        // ── Toolbar ───────────────────────────────────────────────────────────
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // ── Vistas ────────────────────────────────────────────────────────────
        progressBar    = findViewById(R.id.progressBar);
        progressSync   = findViewById(R.id.progressSync);
        tvEmpty        = findViewById(R.id.tvEmpty);
        tvContador     = findViewById(R.id.tvContador);
        btnSincronizar = findViewById(R.id.btnSincronizar);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);
        adapter = new PendingInventoryAdapter(this::mostrarDetalle);
        recyclerView.setAdapter(adapter);

        btnSincronizar.setOnClickListener(v -> confirmarSincronizacion());

        // ── ViewModel ─────────────────────────────────────────────────────────
        ViewModelFactory factory = new ViewModelFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(PendingInventoryViewModel.class);

        viewModel.getIsLoading().observe(this, loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getIsSincronizando().observe(this, syncing -> {
            progressSync.setVisibility(syncing ? View.VISIBLE : View.GONE);
            btnSincronizar.setEnabled(!syncing);
            btnSincronizar.setText(syncing ? "Sincronizando…" : "Sincronizar Todo");
        });

        viewModel.getPendientes().observe(this, lista -> {
            adapter.setList(lista);
            boolean vacio = (lista == null || lista.isEmpty());
            tvEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
            findViewById(R.id.cardSyncBanner).setVisibility(vacio ? View.GONE : View.VISIBLE);
            findViewById(R.id.llHeaderPendientes).setVisibility(vacio ? View.GONE : View.VISIBLE);
            
            if (!vacio) {
                int count = lista.size();
                tvContador.setText(count + " registro(s) pendiente(s) de sincronizar");
            }
        });

        viewModel.getSyncResultMessage().observe(this, msg -> {
            if (msg != null) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Resultado de sincronización")
                        .setMessage(msg)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });

        viewModel.cargarPendientes();
    }

    private void confirmarSincronizacion() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sincronizar registros")
                .setMessage("Se intentará enviar todos los registros pendientes al servidor.\n\nNecesitas conexión a internet. ¿Continuar?")
                .setPositiveButton("Sincronizar", (d, w) -> viewModel.sincronizarTodos())
                .setNegativeButton("Cancelar", null)
                .show();
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

        // Título personalizado para pendientes
        ((TextView) dialogView.findViewById(R.id.tvDialogSubtitle)).setText(inv.getProducto());
        
        // Poblar datos generales
        ((TextView) dialogView.findViewById(R.id.tvDetailCodigo)).setText(inv.getIdProducto());
        
        String unidad = (inv.getUnidadMedida() != null) ? inv.getUnidadMedida() : "UND";
        ((TextView) dialogView.findViewById(R.id.tvDetailUnidad)).setText(unidad);
        ((TextView) dialogView.findViewById(R.id.tvDetailUnidadStock)).setText(unidad);
        ((TextView) dialogView.findViewById(R.id.tvDetailUnidadContado)).setText(unidad);

        ((TextView) dialogView.findViewById(R.id.tvDetailStockSistema)).setText(formatNum(inv.getStock()));
        ((TextView) dialogView.findViewById(R.id.tvDetailCantidadContada)).setText(formatNum(inv.getCantidad()));

        // Detalles de ubicación y usuario
        ((TextView) dialogView.findViewById(R.id.tvDetailAlmacen)).setText(inv.getAlmacen());
        ((TextView) dialogView.findViewById(R.id.tvDetailSucursal)).setText(inv.getSucursal());
        ((TextView) dialogView.findViewById(R.id.tvDetailUser)).setText(inv.getUsuarioCreacion());

        // Formateo de Fecha y Hora
        String fechaRaw = (inv.getFechaCreacion() != null) ? inv.getFechaCreacion() : inv.getFechaRegistroLocal();
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