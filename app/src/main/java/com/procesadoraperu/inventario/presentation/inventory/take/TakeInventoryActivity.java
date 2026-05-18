package com.procesadoraperu.inventario.presentation.inventory.take;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.domain.model.producto.Producto;
import com.procesadoraperu.inventario.presentation.ViewModelFactory;

public class TakeInventoryActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private TakeInventoryViewModel viewModel;
    private View rootView;

    private TextInputEditText etCodigoManual;
    private ProgressBar progressBuscar;

    private CardView cardProducto;
    private TextView tvNombreProducto, tvIdProducto, tvUnidadMedida, tvStockSistema;
    private TextInputEditText etCantidadContada;
    private MaterialButton btnRegistrar;
    private ProgressBar progressRegistrar;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    String codigo = result.getContents().trim();
                    etCodigoManual.setText(codigo);
                    viewModel.buscarProducto(codigo);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_inventory);

        rootView = findViewById(android.R.id.content);

        // ── Toolbar ───────────────────────────────────────────────────────────
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Toma de Inventario");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // ── Ajuste de insets para teclado ─────────────────────────────────────
        View scrollContent = findViewById(R.id.scrollContent);
        if (scrollContent != null) {
            ViewCompat.setOnApplyWindowInsetsListener(scrollContent, (v, insets) -> {
                int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                int navBar    = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                        v.getPaddingRight(), Math.max(imeHeight, navBar) + 16);
                return insets;
            });
        }

        // ── Vistas ────────────────────────────────────────────────────────────
        etCodigoManual  = findViewById(R.id.etCodigoManual);
        progressBuscar  = findViewById(R.id.progressBuscar);
        cardProducto    = findViewById(R.id.cardProducto);
        tvNombreProducto  = findViewById(R.id.tvNombreProducto);
        tvIdProducto      = findViewById(R.id.tvIdProducto);
        tvUnidadMedida    = findViewById(R.id.tvUnidadMedida);
        tvStockSistema    = findViewById(R.id.tvStockSistema);
        etCantidadContada = findViewById(R.id.etCantidadContada);
        btnRegistrar      = findViewById(R.id.btnRegistrar);
        progressRegistrar = findViewById(R.id.progressRegistrar);

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

    private void observarViewModel() {
        viewModel.getIsLoadingProducto().observe(this, loading -> {
            progressBuscar.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (loading) cardProducto.setVisibility(View.GONE);
        });

        viewModel.getProductoEncontrado().observe(this, producto -> {
            if (producto != null) mostrarProducto(producto);
            else cardProducto.setVisibility(View.GONE);
        });

        viewModel.getIsRegistrando().observe(this, loading -> {
            progressRegistrar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnRegistrar.setEnabled(!loading);
            btnRegistrar.setText(loading ? "Enviando…" : "REGISTRAR INVENTARIO");
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error == null || error.isEmpty()) return;
            Snackbar snack = Snackbar.make(rootView, error, Snackbar.LENGTH_LONG);
            snack.setBackgroundTint(getColor(R.color.pp_error));
            snack.setTextColor(getColor(R.color.pp_white));
            snack.show();
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
                            "📴 Sin conexión a internet.\n\n" +
                                    "El registro fue guardado en el dispositivo. " +
                                    "Se enviará automáticamente al servidor cuando recuperes la conexión.\n\n" +
                                    "Puedes verlo en la sección «Registros Pendientes».");
                    break;
                case ERROR:
                    break;
            }
        });
    }

    private void mostrarProducto(Producto producto) {
        tvNombreProducto.setText(producto.getDescripcion());
        tvIdProducto.setText("Código: " + producto.getIdProducto());
        tvUnidadMedida.setText("U.M.: " + producto.getIdMedida());

        double stockVal = producto.getStock().doubleValue();
        String stockStr = (stockVal == Math.floor(stockVal))
                ? String.valueOf((int) stockVal) : String.valueOf(stockVal);
        tvStockSistema.setText("Stock en sistema: " + stockStr + " " + producto.getIdMedida());

        etCantidadContada.setText("");
        cardProducto.setVisibility(View.VISIBLE);
        etCantidadContada.requestFocus();
    }

    private void registrarInventario() {
        Producto producto = viewModel.getProductoEncontrado().getValue();
        if (producto == null) {
            Snackbar.make(rootView, "Primero busca un producto", Snackbar.LENGTH_SHORT).show();
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
            if (cantidad < 0) { etCantidadContada.setError("La cantidad no puede ser negativa"); return; }
            viewModel.registrarInventario(producto, cantidad);
        } catch (NumberFormatException e) {
            etCantidadContada.setError("Cantidad inválida");
        }
    }

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

    private void solicitarCamaraYEscanear() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            lanzarEscaner();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            lanzarEscaner();
        } else {
            Snackbar.make(rootView,
                    "Se necesita permiso de cámara para escanear códigos",
                    Snackbar.LENGTH_LONG).show();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}