package com.procesadoraperu.inventario.presentation.inventory.take;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.EditorInfo;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.procesadoraperu.inventario.core.network.InternetUtil;
import com.procesadoraperu.inventario.domain.model.producto.Producto;
import com.procesadoraperu.inventario.presentation.ViewModelFactory;

public class TakeInventoryActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_LOCATION_PERMISSION = 101;

    private TakeInventoryViewModel viewModel;

    private TextInputEditText etCodigoManual;
    private ProgressBar progressBuscar;

    private MaterialCardView cardProducto;
    private MaterialCardView cardOfflineBanner;
    private View llHeroBackground;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private TextView tvNombreProducto, tvIdProducto, tvStockSistema;
    private com.google.android.material.textfield.TextInputLayout tilCantidadContada;
    private TextInputEditText etCantidadContada;
    private MaterialButton btnRegistrar;
    private ProgressBar progressRegistrar;

    /* ── Launcher para volver de la pantalla de ajustes de ubicación ────── */
    private final ActivityResultLauncher<Intent> locationSettingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // Al volver de ajustes, reintentar el registro si el GPS ya está activo
                if (isGpsEnabled()) {
                    ejecutarRegistro();
                } else {
                    mostrarSnackbar("La ubicación sigue desactivada. Actívala para registrar.", true);
                }
            });

    /* ── Escáner de código de barras / QR ─────────────────────────────────── */
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    String codigo = result.getContents().trim();
                    etCodigoManual.setText(codigo);
                    viewModel.buscarProducto(codigo, InternetUtil.hayInternet(this));
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
        registrarMonitorDeRed();
    }

    private void initViews() {
        etCodigoManual   = findViewById(R.id.etCodigoManual);
        progressBuscar   = findViewById(R.id.progressBuscar);
        cardProducto     = findViewById(R.id.cardProducto);
        cardOfflineBanner = findViewById(R.id.cardOfflineBanner);
        llHeroBackground = findViewById(R.id.llHeroBackground);
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
        viewModel.buscarProducto(codigo, InternetUtil.hayInternet(this));
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

            // Verificar permiso de ubicación antes de registrar
            if (!tienePermisoUbicacion()) {
                solicitarPermisoUbicacion();
                return;
            }

            // Verificar que el GPS esté activo
            if (!isGpsEnabled()) {
                mostrarDialogoActivarGps();
                return;
            }

            ejecutarRegistro();
        } catch (NumberFormatException e) {
            etCantidadContada.setError("Formato inválido (use números)");
        }
    }

    /**
     * Ejecuta el registro una vez que se verificó que hay permiso y GPS activo.
     */
    private void ejecutarRegistro() {
        Producto producto = viewModel.getProductoEncontrado().getValue();
        String cantidadStr = (etCantidadContada.getText() != null)
                ? etCantidadContada.getText().toString().trim() : "";
        if (producto == null || cantidadStr.isEmpty()) return;
        try {
            double cantidad = Double.parseDouble(cantidadStr);
            viewModel.registrarInventario(producto, cantidad);
        } catch (NumberFormatException ignored) {}
    }

    /* ── Verificación de ubicación ───────────────────────────────────────── */

    private boolean tienePermisoUbicacion() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isGpsEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) return false;
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void solicitarPermisoUbicacion() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, REQUEST_LOCATION_PERMISSION);
    }

    private void mostrarDialogoActivarGps() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Ubicación desactivada")
                .setMessage("Para registrar el inventario se necesita la ubicación activa. ¿Deseas activarla?")
                .setPositiveButton("Activar", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    locationSettingsLauncher.launch(intent);
                })
                .setNegativeButton("Cancelar", null)
                .setCancelable(false)
                .show();
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

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                lanzarEscaner();
            } else {
                mostrarSnackbar("Se necesita permiso de cámara para escanear", true);
            }
        } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permiso concedido, verificar si el GPS está activo
                if (isGpsEnabled()) {
                    ejecutarRegistro();
                } else {
                    mostrarDialogoActivarGps();
                }
            } else {
                mostrarSnackbar("Se necesita permiso de ubicación para registrar inventario", true);
            }
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

    @Override
    protected void onResume() {
        super.onResume();
        actualizarEstadoConexion();
    }

    private void actualizarEstadoConexion() {
        boolean sinInternet = !InternetUtil.hayInternet(this);
        cardOfflineBanner.setVisibility(sinInternet ? View.VISIBLE : View.GONE);

        // Cambiar color del hero card cuando no hay conexión
        if (sinInternet) {
            llHeroBackground.setBackgroundColor(getColor(R.color.pp_splash_orange));
        } else {
            llHeroBackground.setBackgroundColor(getColor(R.color.pp_splash_text_green));
        }
    }

    private void registrarMonitorDeRed() {
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@androidx.annotation.NonNull Network network) {
                runOnUiThread(() -> actualizarEstadoConexion());
            }

            @Override
            public void onLost(@androidx.annotation.NonNull Network network) {
                runOnUiThread(() -> actualizarEstadoConexion());
            }
        };

        NetworkRequest request = new NetworkRequest.Builder().build();
        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }
}