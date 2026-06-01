package com.procesadoraperu.inventario.presentation.inventory.history;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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