package com.procesadoraperu.inventario.presentation.home;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.core.sync.SyncScheduler;
import com.procesadoraperu.inventario.presentation.ViewModelFactory;
import com.procesadoraperu.inventario.presentation.auth.LoginActivity;
import com.procesadoraperu.inventario.presentation.inventory.history.InventoryHistoryActivity;
import com.procesadoraperu.inventario.presentation.inventory.pending.PendingInventoryActivity;
import com.procesadoraperu.inventario.presentation.inventory.take.TakeInventoryActivity;
import com.procesadoraperu.inventario.presentation.profile.UserProfileActivity;
import com.procesadoraperu.inventario.presentation.selection.SucursalActivity;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private HomeViewModel viewModel;
    private DrawerLayout drawerLayout;
    private View rootView;

    private TextView tvNavNombre, tvNavSucursal, tvNavAlmacen, tvNavAvatar;
    private TextView tvToolbarSubtitle;
    private TextView tvCatalogCount;
    private MaterialButton btnClearCatalog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Forzamos que los iconos de la barra de estado sean blancos sobre el fondo verde
        if (getWindow() != null) {
            new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                    .setAppearanceLightStatusBars(false);
        }

        drawerLayout = findViewById(R.id.drawerLayoutRoot);
        rootView = drawerLayout;

        // Manejo de Insets para pantalla completa (Edge-to-Edge)
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            // Aplicamos padding al AppBar para que no choque con la status bar
            findViewById(R.id.appbar).setPadding(0, systemBars.top, 0, 0);
            
            // Los lados y el fondo se manejan en el contenedor raíz si es necesario
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            
            return WindowInsetsCompat.CONSUMED;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("");
        tvToolbarSubtitle = findViewById(R.id.tvToolbarSubtitle);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        setupUI();
        setupViewModel();
        
        if (!hayConexion()) mostrarSnackbarOffline();

        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() {
                        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                            drawerLayout.closeDrawer(GravityCompat.START);
                        } else {
                            finish();
                        }
                    }
                });
    }

    private void setupUI() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        tvNavAvatar   = headerView.findViewById(R.id.tvNavAvatar);
        tvNavNombre   = headerView.findViewById(R.id.tvNavNombre);
        tvNavSucursal = headerView.findViewById(R.id.tvNavSucursal);
        tvNavAlmacen  = headerView.findViewById(R.id.tvNavAlmacen);

        tvCatalogCount  = findViewById(R.id.tvCatalogCount);
        btnClearCatalog = findViewById(R.id.btnClearCatalog);

        findViewById(R.id.btnRealizarToma).setOnClickListener(v ->
                startActivity(new Intent(this, TakeInventoryActivity.class)));

        CardView cardHistorial  = findViewById(R.id.cardHistorial);
        CardView cardPendientes = findViewById(R.id.cardPendientes);
        if (cardHistorial  != null) cardHistorial.setOnClickListener(v ->
                startActivity(new Intent(this, InventoryHistoryActivity.class)));
        if (cardPendientes != null) cardPendientes.setOnClickListener(v ->
                startActivity(new Intent(this, PendingInventoryActivity.class)));

        btnClearCatalog.setOnClickListener(v -> mostrarDialogoConfirmarLimpiar());
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(HomeViewModel.class);

        viewModel.getHeaderData().observe(this, data -> {
            String inicial = (data.nombreUsuario != null && !data.nombreUsuario.isEmpty())
                    ? String.valueOf(data.nombreUsuario.charAt(0)).toUpperCase() : "O";
            tvNavAvatar.setText(inicial);
            tvNavNombre.setText(data.nombreUsuario);
            tvNavSucursal.setText(data.sucursal);
            tvNavAlmacen.setText(data.almacen);
            tvToolbarSubtitle.setText(data.sucursal + "  ·  " + data.almacen);
        });

        viewModel.getLogoutSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                // Cancelar sincronización automática al cerrar sesión
                SyncScheduler.cancel(this);

                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        viewModel.getCatalogCount().observe(this, count -> {
            if (count == null || count == 0) {
                tvCatalogCount.setText("Sin catálogo offline");
            } else {
                tvCatalogCount.setText(count + " productos disponibles offline");
            }
        });

        viewModel.getClearSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Catálogo limpiado exitosamente", Toast.LENGTH_SHORT).show();
            } else if (success != null) {
                Toast.makeText(this, "No se pudo limpiar el catálogo. Intenta de nuevo.", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.cargarDatosCabecera();
    }

    private void mostrarSnackbarOffline() {
        Snackbar snack = Snackbar.make(rootView, R.string.msg_guardado_local, Snackbar.LENGTH_INDEFINITE);
        snack.setBackgroundTint(getColor(R.color.pp_warning));
        snack.setTextColor(getColor(R.color.pp_white));
        snack.setAction("Entendido", v -> snack.dismiss());
        snack.show();
    }

    private boolean hayConexion() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_historial) startActivity(new Intent(this, InventoryHistoryActivity.class));
        else if (id == R.id.nav_pendientes) startActivity(new Intent(this, PendingInventoryActivity.class));
        else if (id == R.id.nav_toma) startActivity(new Intent(this, TakeInventoryActivity.class));
        else if (id == R.id.nav_cambiar_ubicacion) {
            Intent intent = new Intent(this, SucursalActivity.class);
            intent.putExtra("CAMBIO_UBICACION", true);
            startActivity(intent);
        } else if (id == R.id.nav_perfil) startActivity(new Intent(this, UserProfileActivity.class));
        else if (id == R.id.nav_cerrar_sesion) confirmarCerrarSesion();

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void confirmarCerrarSesion() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro que deseas cerrar sesión?")
                .setPositiveButton("Sí, cerrar", (d, w) -> viewModel.cerrarSesion())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoConfirmarLimpiar() {
        new AlertDialog.Builder(this)
                .setTitle("Limpiar Catálogo")
                .setMessage("Se eliminarán todos los datos del catálogo offline")
                .setPositiveButton("Confirmar", (d, w) -> viewModel.clearCatalog())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.cargarDatosCabecera();
        viewModel.loadCatalogCount();
    }
}