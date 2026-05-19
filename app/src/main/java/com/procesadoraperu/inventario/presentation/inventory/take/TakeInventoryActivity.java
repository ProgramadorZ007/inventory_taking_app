package com.procesadoraperu.inventario.presentation.inventory.take;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
    private View rootView;

    private TextInputEditText etCodigoManual;
    private ProgressBar progressBuscar;

    private MaterialCardView cardProducto;
    private TextView tvNombreProducto, tvIdProducto, tvStockSistema;
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
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_inventory);

        rootView = findViewById(R.id.take_inventory_container);

        // Configurar Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Manejo de Safe Area e Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.take_inventory_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });

        initViews();
        setupViewModel();
    }

    private void initViews() {
        etCodigoManual = findViewById(R.id.etCodigoManual);
        progressBuscar = findViewById(R.id.progressBuscar);
        cardProducto = findViewById(R.id.cardProducto);
        tvNombreProducto = findViewById(R.id.tvNombreProducto);
        tvIdProducto = findViewById(R.id.tvIdProducto);
        tvStockSistema = findViewById(R.id.tvStockSistema);
        etCantidadContada = findViewById(R.id.etCantidadContada);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        progressRegistrar = findViewById(R.id.progressRegistrar);

        findViewById(R.id.btnEscanear).setOnClickListener(v -> solicitarCamaraYEscanear());
        findViewById(R.id.btnBuscarManual).setOnClickListener(v -> {
            String codigo = etCodigoManual.getText().toString().trim();
            if (codigo.isEmpty()) {
                etCodigoManual.setError("Ingrese un código");
                return;
            }
            viewModel.buscarProducto(codigo);
        });

        btnRegistrar.setOnClickListener(v -> registrarInventario());
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
            if (error != null) {
                Snackbar.make(rootView, error, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getColor(R.color.pp_error))
                        .show();
            }
        });

        viewModel.getRegistroResult().observe(this, result -> {
            if (result == null) return;
            switch (result) {
                case SINCRONIZADO:
                    mostrarResultado(true, "Registro enviado correctamente al servidor.");
                    break;
                case GUARDADO_LOCAL:
                    mostrarResultado(false, "Sin conexión. Guardado en el dispositivo.");
                    break;
            }
        });
    }

    private void mostrarProducto(Producto producto) {
        tvNombreProducto.setText(producto.getDescripcion());
        tvIdProducto.setText("Código: " + producto.getIdProducto());
        tvStockSistema.setText("Stock actual: " + producto.getStock() + " " + producto.getIdMedida());
        cardProducto.setVisibility(View.VISIBLE);
        etCantidadContada.requestFocus();
    }

    private void registrarInventario() {
        Producto producto = viewModel.getProductoEncontrado().getValue();
        String cantidadStr = etCantidadContada.getText().toString().trim();

        if (producto == null) return;
        if (cantidadStr.isEmpty()) {
            etCantidadContada.setError("Ingrese cantidad");
            return;
        }

        try {
            double cantidad = Double.parseDouble(cantidadStr);
            viewModel.registrarInventario(producto, cantidad);
        } catch (NumberFormatException e) {
            etCantidadContada.setError("Formato inválido");
        }
    }

    private void mostrarResultado(boolean exito, String mensaje) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(exito ? "Éxito" : "Guardado Local")
                .setMessage(mensaje)
                .setPositiveButton("Nuevo registro", (d, w) -> {
                    viewModel.limpiarEstado();
                    etCodigoManual.setText("");
                    cardProducto.setVisibility(View.GONE);
                })
                .setNegativeButton("Finalizar", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private void solicitarCamaraYEscanear() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            lanzarEscaner();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            lanzarEscaner();
        }
    }

    private void lanzarEscaner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Escanea el producto");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        barcodeLauncher.launch(options);
    }
}