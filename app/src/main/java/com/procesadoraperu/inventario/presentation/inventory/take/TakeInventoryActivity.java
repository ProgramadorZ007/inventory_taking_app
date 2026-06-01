package com.procesadoraperu.inventario.presentation.inventory.take;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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

    private TextInputEditText etCodigoManual;
    private ProgressBar progressBuscar;

    private MaterialCardView cardProducto;
    private TextView tvNombreProducto, tvIdProducto, tvStockSistema;
    private com.google.android.material.textfield.TextInputLayout tilCantidadContada;
    private TextInputEditText etCantidadContada;
    private MaterialButton btnRegistrar;
    private ProgressBar progressRegistrar;

    /* ── Escáner de código de barras / QR ─────────────────────────────────── */
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    String codigo = result.getContents().trim();
                    etCodigoManual.setText(codigo);
                    viewModel.buscarProducto(codigo);
                } else {
                    // El usuario canceló el escaneo
                    mostrarSnackbar("Escaneo cancelado", false);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_inventory);

        // Forzamos que los iconos de la barra de estado sean blancos sobre el fondo verde oscuro
        if (getWindow() != null) {
            new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                    .setAppearanceLightStatusBars(false);
        }

        // ✔️ MANEJO DE INSETS: El color verde sube hasta la barra de estado
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.take_inventory_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            // 1. El AppBarLayout recibirá el padding superior para que el texto no choque con la hora/iconos
            findViewById(R.id.appbar).setPadding(0, systemBars.top, 0, 0);

            // 2. El contenedor raíz solo maneja los lados y el teclado (abajo)
            v.setPadding(systemBars.left, 0, systemBars.right, Math.max(systemBars.bottom, ime.bottom));

            return WindowInsetsCompat.CONSUMED;
        });

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();
        setupViewModel();
    }

    private void initViews() {
        etCodigoManual   = findViewById(R.id.etCodigoManual);
        progressBuscar   = findViewById(R.id.progressBuscar);
        cardProducto     = findViewById(R.id.cardProducto);
        tvNombreProducto = findViewById(R.id.tvNombreProducto);
        tvIdProducto     = findViewById(R.id.tvIdProducto);
        tvStockSistema   = findViewById(R.id.tvStockSistema);
        tilCantidadContada = findViewById(R.id.tilCantidadContada);
        etCantidadContada = findViewById(R.id.etCantidadContada);
        btnRegistrar     = findViewById(R.id.btnRegistrar);
        progressRegistrar = findViewById(R.id.progressRegistrar);

        // Escanear con cámara
        findViewById(R.id.btnEscanear).setOnClickListener(v -> solicitarCamaraYEscanear());

        // Buscar manualmente
        findViewById(R.id.btnBuscarManual).setOnClickListener(v -> buscarManual());

        // También buscar al presionar "Buscar" en el teclado
        etCodigoManual.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE) {
                buscarManual();
                return true;
            }
            return false;
        });

        btnRegistrar.setOnClickListener(v -> registrarInventario());
    }

    private void buscarManual() {
        String codigo = (etCodigoManual.getText() != null)
                ? etCodigoManual.getText().toString().trim() : "";
        if (codigo.isEmpty()) {
            etCodigoManual.setError("Ingrese un código");
            return;
        }
        viewModel.buscarProducto(codigo);
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(TakeInventoryViewModel.class);

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
            btnRegistrar.setText(loading ? "Enviando..." : "REGISTRAR CONTEO");
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                mostrarSnackbar(error, true);
                viewModel.limpiarEstado();
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
                            "⚠️ Sin conexión. Guardado en el dispositivo.\nSe enviará al recuperar internet.");
                    break;
                case ERROR:
                    // El error ya fue mostrado en errorMessage
                    break;
            }
        });
    }

    private void mostrarProducto(Producto producto) {
        tvNombreProducto.setText(producto.getDescripcion() != null
                ? producto.getDescripcion() : "Sin descripción");
        tvIdProducto.setText("Código: " + producto.getIdProducto());
        
        // REQUISITO: Mostrar solo la unidad de medida, ocultando el stock del sistema
        String unidad = (producto.getIdMedida() != null) ? producto.getIdMedida() : "UND";
        tvStockSistema.setText(unidad);
        tilCantidadContada.setSuffixText(unidad);

        cardProducto.setVisibility(View.VISIBLE);
        etCantidadContada.requestFocus();
    }

    private void registrarInventario() {
        Producto producto = viewModel.getProductoEncontrado().getValue();
        String cantidadStr = (etCantidadContada.getText() != null)
                ? etCantidadContada.getText().toString().trim() : "";

        if (producto == null) {
            mostrarSnackbar("Primero busca un producto", false);
            return;
        }
        if (cantidadStr.isEmpty()) {
            etCantidadContada.setError("Ingrese la cantidad");
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
            etCantidadContada.setError("Formato inválido (use números)");
        }
    }

    private void mostrarResultado(boolean exito, String mensaje) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(exito ? "Éxito" : "Guardado Local")
                .setMessage(mensaje)
                .setPositiveButton("Nuevo registro", (d, w) -> {
                    viewModel.limpiarEstado();
                    etCodigoManual.setText("");
                    etCantidadContada.setText("");
                    cardProducto.setVisibility(View.GONE);
                })
                .setNegativeButton("Finalizar", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private void mostrarSnackbar(String mensaje, boolean esError) {
        View root = findViewById(R.id.take_inventory_container);
        if (root == null) return;
        Snackbar snack = Snackbar.make(root, mensaje, Snackbar.LENGTH_LONG);
        if (esError) snack.setBackgroundTint(getColor(R.color.pp_error));
        snack.show();
    }

    /* ── Cámara ─────────────────────────────────────────────────────────────── */

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
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            lanzarEscaner();
        } else {
            mostrarSnackbar("Se necesita permiso de cámara para escanear", true);
        }
    }

    private void lanzarEscaner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Escanea el código del producto");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        // Soporta tanto QR como código de barras
        options.setDesiredBarcodeFormats(
                ScanOptions.QR_CODE,
                ScanOptions.CODE_128,
                ScanOptions.CODE_39,
                ScanOptions.EAN_13,
                ScanOptions.EAN_8
        );
        barcodeLauncher.launch(options);
    }
}