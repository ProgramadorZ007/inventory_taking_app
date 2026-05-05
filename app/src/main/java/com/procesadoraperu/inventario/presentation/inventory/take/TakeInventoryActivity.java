package com.procesadoraperu.inventario.presentation.inventory.take;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.domain.model.producto.Producto;
import com.procesadoraperu.inventario.presentation.ViewModelFactory;

public class TakeInventoryActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private TakeInventoryViewModel viewModel;

    // ── Vistas de búsqueda ────────────────────────────────────────────────────
    private TextInputEditText etCodigoManual;
    private ProgressBar progressBuscar;

    // ── Vistas de producto encontrado ─────────────────────────────────────────
    private CardView cardProducto;
    private TextView tvNombreProducto, tvIdProducto, tvUnidadMedida, tvStockSistema;
    private TextInputEditText etCantidadContada;
    private MaterialButton btnRegistrar;
    private ProgressBar progressRegistrar;

    // ── Escáner de código de barras (ZXing Android Embedded) ─────────────────
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    String codigoEscaneado = result.getContents().trim();
                    etCodigoManual.setText(codigoEscaneado);
                    viewModel.buscarProducto(codigoEscaneado);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_inventory);

        // ── Toolbar ───────────────────────────────────────────────────────────
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Toma de Inventario");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // ── Vistas de búsqueda ────────────────────────────────────────────────
        etCodigoManual = findViewById(R.id.etCodigoManual);
        progressBuscar = findViewById(R.id.progressBuscar);

        // ── Vistas de producto encontrado ─────────────────────────────────────
        cardProducto       = findViewById(R.id.cardProducto);
        tvNombreProducto   = findViewById(R.id.tvNombreProducto);
        tvIdProducto       = findViewById(R.id.tvIdProducto);
        tvUnidadMedida     = findViewById(R.id.tvUnidadMedida);
        tvStockSistema     = findViewById(R.id.tvStockSistema);
        etCantidadContada  = findViewById(R.id.etCantidadContada);
        btnRegistrar       = findViewById(R.id.btnRegistrar);
        progressRegistrar  = findViewById(R.id.progressRegistrar);

        // ── Listeners ─────────────────────────────────────────────────────────
        findViewById(R.id.btnEscanear).setOnClickListener(v -> solicitarCamaraYEscanear());

        findViewById(R.id.btnBuscarManual).setOnClickListener(v -> {
            String codigo = etCodigoManual.getText() != null
                    ? etCodigoManual.getText().toString().trim() : "";
            if (codigo.isEmpty()) {
                etCodigoManual.setError("Ingresa el código del producto");
                return;
            }
            viewModel.buscarProducto(codigo);
        });

        btnRegistrar.setOnClickListener(v -> registrarInventario());

        // ── ViewModel ─────────────────────────────────────────────────────────
        ViewModelFactory factory = new ViewModelFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(TakeInventoryViewModel.class);

        observarViewModel();
    }

    // ── Observadores ──────────────────────────────────────────────────────────

    private void observarViewModel() {
        viewModel.getIsLoadingProducto().observe(this, loading -> {
            progressBuscar.setVisibility(loading ? View.VISIBLE : View.GONE);
            // Ocultar la card mientras se carga
            if (loading) cardProducto.setVisibility(View.GONE);
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
            btnRegistrar.setText(loading ? "Enviando…" : "REGISTRAR INVENTARIO");
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
                    mostrarResultado(true,
                            "✅ Registro enviado correctamente al servidor.");
                    break;
                case GUARDADO_LOCAL:
                    mostrarResultado(false,
                            "⚠️ Sin conexión a internet.\n\nEl registro fue guardado localmente " +
                                    "y se enviará automáticamente cuando haya conexión.");
                    break;
                case ERROR:
                    // El error ya se mostró via errorMessage
                    break;
            }
        });
    }

    // ── Mostrar datos del producto ────────────────────────────────────────────

    private void mostrarProducto(Producto producto) {
        tvNombreProducto.setText(producto.getDescripcion());
        tvIdProducto.setText("Código: " + producto.getIdProducto());
        tvUnidadMedida.setText("U.M.: " + producto.getIdMedida());

        double stockVal = producto.getStock().doubleValue();
        String stockStr = (stockVal == Math.floor(stockVal))
                ? String.valueOf((int) stockVal)
                : String.valueOf(stockVal);
        tvStockSistema.setText("Stock en sistema: " + stockStr + " " + producto.getIdMedida());

        etCantidadContada.setText("");
        cardProducto.setVisibility(View.VISIBLE);
        etCantidadContada.requestFocus();
    }

    // ── Registro ──────────────────────────────────────────────────────────────

    private void registrarInventario() {
        Producto producto = viewModel.getProductoEncontrado().getValue();
        if (producto == null) {
            Toast.makeText(this, "Primero busca un producto", Toast.LENGTH_SHORT).show();
            return;
        }

        String cantidadStr = etCantidadContada.getText() != null
                ? etCantidadContada.getText().toString().trim() : "";

        if (cantidadStr.isEmpty()) {
            etCantidadContada.setError("Ingresa la cantidad contada");
            return;
        }

        try {
            double cantidad = Double.parseDouble(cantidadStr);
            if (cantidad < 0) {
                etCantidadContada.setError("La cantidad no puede ser negativa");
                return;
            }
            viewModel.registrarInventario(producto, cantidad);
        } catch (NumberFormatException e) {
            etCantidadContada.setError("Cantidad inválida");
        }
    }

    // ── Diálogo resultado ─────────────────────────────────────────────────────

    private void mostrarResultado(boolean exito, String mensaje) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(exito ? "✅ Registro exitoso" : "📴 Guardado sin conexión")
                .setMessage(mensaje)
                .setPositiveButton("Registrar otro", (d, w) -> {
                    viewModel.limpiarEstado();
                    etCodigoManual.setText("");
                    etCantidadContada.setText("");
                    cardProducto.setVisibility(View.GONE);
                })
                .setNegativeButton("Volver al inicio", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    // ── Cámara y escáner ──────────────────────────────────────────────────────

    private void solicitarCamaraYEscanear() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            lanzarEscaner();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                lanzarEscaner();
            } else {
                Toast.makeText(this,
                        "Se necesita permiso de cámara para escanear códigos",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void lanzarEscaner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Apunta al código de barras del producto");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setBarcodeImageEnabled(false);
        barcodeLauncher.launch(options);
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}