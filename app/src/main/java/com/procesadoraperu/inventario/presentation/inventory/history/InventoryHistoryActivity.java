package com.procesadoraperu.inventario.presentation.inventory.history;

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

import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.presentation.ViewModelFactory;

public class InventoryHistoryActivity extends AppCompatActivity {

    private InventoryHistoryViewModel viewModel;
    private InventoryHistoryAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_history);

        // CORRECCIÓN: El AppBarLayout en el XML tiene fitsSystemWindows=true,
        // así el toolbar ya respeta la status bar sin necesidad de EdgeToEdge
        // ni listeners manuales de insets.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mis Inventarios de Hoy");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

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

        viewModel.cargarHistorialHoy();
    }

    private void mostrarDetalle(
            com.procesadoraperu.inventario.domain.model.inventario.Inventario inv) {
        String detalle =
                "Producto: " + inv.getProducto() + "\n" +
                        "Código: "   + inv.getIdProducto() + "\n" +
                        "U.M.: "     + inv.getUnidadMedida() + "\n\n" +
                        "Stock sistema: "   + inv.getStock() + "\n" +
                        "Cantidad contada: " + inv.getCantidad() + "\n\n" +
                        "Almacén: "  + inv.getAlmacen() + "\n" +
                        "Sucursal: " + inv.getSucursal() + "\n" +
                        "Registrado por: " + inv.getUsuarioCreacion() + "\n" +
                        "Fecha: " + (inv.getFechaCreacion() != null
                        ? inv.getFechaCreacion().replace("T", " ") : "—");

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Detalle del registro")
                .setMessage(detalle)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}