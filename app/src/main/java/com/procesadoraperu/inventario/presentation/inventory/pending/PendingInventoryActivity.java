package com.procesadoraperu.inventario.presentation.inventory.pending;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.domain.model.inventario.Inventario;
import com.procesadoraperu.inventario.presentation.ViewModelFactory;

public class PendingInventoryActivity extends AppCompatActivity {

    private PendingInventoryViewModel viewModel;
    private PendingInventoryAdapter adapter;

    private ProgressBar progressBar, progressSync;
    private TextView tvEmpty, tvContador;
    private MaterialButton btnSincronizar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_inventory);

        // ── Toolbar ───────────────────────────────────────────────────────────
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Registros Pendientes");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

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
            btnSincronizar.setVisibility(vacio ? View.GONE : View.VISIBLE);
            tvContador.setVisibility(vacio ? View.GONE : View.VISIBLE);
            int count = vacio ? 0 : lista.size();
            tvContador.setText(count + " registro(s) pendiente(s) de sincronizar");
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
        String detalle =
                "📦  Producto:  " + (inv.getProducto() != null ? inv.getProducto() : "—") + "\n" +
                        "🔖  Código:  "   + (inv.getIdProducto() != null ? inv.getIdProducto() : "—") + "\n" +
                        "🧮  Cantidad:  " + formatNum(inv.getCantidad()) + " " +
                        (inv.getUnidadMedida() != null ? inv.getUnidadMedida() : "") + "\n" +
                        "🏢  Almacén:  "  + (inv.getAlmacen() != null ? inv.getAlmacen() : "—") + "\n" +
                        "🏭  Sucursal:  " + (inv.getSucursal() != null ? inv.getSucursal() : "—") + "\n\n" +
                        "⏰  Registrado:  " + (inv.getFechaRegistroLocal() != null
                        ? inv.getFechaRegistroLocal() : "—");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Detalle del Pendiente")
                .setMessage(detalle)
                .setPositiveButton("Cerrar", null)
                .show();
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