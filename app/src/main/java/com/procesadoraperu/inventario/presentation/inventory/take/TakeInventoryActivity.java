package com.procesadoraperu.inventario.presentation.inventory.take;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;
import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.domain.model.producto.Producto;
import com.procesadoraperu.inventario.presentation.ViewModelFactory;

import androidx.activity.result.ActivityResultLauncher;

public class TakeInventoryActivity extends AppCompatActivity {

    private TakeInventoryViewModel viewModel;

    // Vistas de búsqueda
    private TextInputEditText etCodigoManual;
    private ProgressBar progressBuscar;

    // Vistas de producto encontrado
    private CardView cardProducto;
    private TextView tvNombreProducto, tvIdProducto, tvUnidadMedida, tvStockSistema;
    private TextInputEditText etCantidadContada;
    private MaterialButton btnRegistrar;
    private ProgressBar progressRegistrar;

    // Launcher del escáner
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    etCodigoManual.setText(result.getContents());
                    viewModel.buscarProducto(result.getContents());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_inventory);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Toma de Inventario");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Vistas de búsqueda
        etCodigoManual  = findViewById(R.id.etCodigoManual);
        progressBuscar  = findViewById(R.id.progressBuscar);

        // Vistas de resultado
        cardProducto    = findViewById(R.id.cardProducto);
        tvNombreProducto = findViewById(R.id.tvNombreProducto);
        tvIdProducto    = findViewById(R.id.tvIdProducto);
        tvUnidadMedida  = findViewById(R.id.tvUnidadMedida);
        tvStockSistema  = findViewById(R.id.tvStockSistema);
        etCantidadContada = findViewById(R.id.etCantidadContada);
        btnRegistrar    = findViewById(R.id.btnRegistrar);
        progressRegistrar = findViewById(R.id.progressRegistrar);

        // Botones
        findViewById(R.id.btnEscanear).setOnClickListener(v -> lanzarEscaner());
        findViewById(R.id.btnBuscarManual).setOnClickListener(v -> {
            String codigo = etCodigoManual.getText() != null
                    ? etCodigoManual.getText().toString().trim() : "";
            viewModel.buscarProducto(codigo);
        });
        btnRegistrar.setOnClickListener(v -> registrarInventario());

        // ViewModel
        ViewModelFactory factory = new ViewModelFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(TakeInventoryViewModel.class);

        observarViewModel();
    }

    private void observarViewModel() {
        viewModel.getIsLoadingProducto().observe(this, loading -> {
            progressBuscar.setVisibility(loading ? View.VISIBLE : View.GONE);
            cardProducto.setVisibility(loading ? View.GONE : cardProducto.getVisibility());
        });

        viewModel.getProductoEncontrado().observe(this, producto -> {
            if (producto != null) {
                mostrarProducto(producto);
            } else {
                cardProducto.setVisibility(View.GONE);
            }
        });

        viewModel.getIsRegistrando().observe(this, loading -> {
            progressRegistrar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnRegistrar.setEnabled(!loading);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getRegistroResult().observe(this, result -> {
            if (result == null) return;
            switch (result) {
                case SINCRONIZADO:
                    mostrarResultado(true, "✅ Registro enviado correctamente al servidor.");
                    break;
                case GUARDADO_LOCAL:
                    mostrarResultado(false,
                            "⚠️ Sin conexión. Registro guardado localmente.\nSe enviará cuando haya internet.");
                    break;
                case ERROR:
                    // El error ya se mostró en errorMessage
                    break;
            }
        });
    }

    private void mostrarProducto(Producto producto) {
        tvNombreProducto.setText(producto.getDescripcion());
        tvIdProducto.setText("Código: " + producto.getIdProducto());
        tvUnidadMedida.setText("U.M.: " + producto.getIdMedida());
        tvStockSistema.setText("Stock sistema: " + producto.getStock().toPlainString());
        etCantidadContada.setText("");
        cardProducto.setVisibility(View.VISIBLE);
        etCantidadContada.requestFocus();
    }

    private void registrarInventario() {
        Producto producto = viewModel.getProductoEncontrado().getValue();
        String cantidadStr = etCantidadContada.getText() != null
                ? etCantidadContada.getText().toString().trim() : "";

        if (cantidadStr.isEmpty()) {
            etCantidadContada.setError("Ingresa la cantidad contada");
            return;
        }

        try {
            double cantidad = Double.parseDouble(cantidadStr);
            viewModel.registrarInventario(producto, cantidad);
        } catch (NumberFormatException e) {
            etCantidadContada.setError("Cantidad inválida");
        }
    }

    private void mostrarResultado(boolean exito, String mensaje) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(exito ? "Registro exitoso" : "Guardado sin conexión")
                .setMessage(mensaje)
                .setPositiveButton("Registrar otro", (d, w) -> {
                    viewModel.limpiarEstado();
                    etCodigoManual.setText("");
                    etCantidadContada.setText("");
                })
                .setNegativeButton("Volver", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private void lanzarEscaner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Apunta al código de barras del producto");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(null); // Usa el activity por defecto de ZXing
        barcodeLauncher.launch(options);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}