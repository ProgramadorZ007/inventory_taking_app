package com.procesadoraperu.inventario.presentation.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.navigation.NavigationView;
import com.procesadoraperu.inventario.R;
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

    // Header del Navigation Drawer
    private TextView tvNavNombre, tvNavSucursal, tvNavAlmacen, tvNavAvatar;
    // Toolbar
    private TextView tvToolbarSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // ── Toolbar ───────────────────────────────────────────────────────────
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("");
        tvToolbarSubtitle = findViewById(R.id.tvToolbarSubtitle);

        // ── Navigation Drawer ────────────────────────────────────────────────
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Vistas del header del drawer
        View headerView = navigationView.getHeaderView(0);
        tvNavAvatar   = headerView.findViewById(R.id.tvNavAvatar);
        tvNavNombre   = headerView.findViewById(R.id.tvNavNombre);
        tvNavSucursal = headerView.findViewById(R.id.tvNavSucursal);
        tvNavAlmacen  = headerView.findViewById(R.id.tvNavAlmacen);

        // ── Botón principal ───────────────────────────────────────────────────
        findViewById(R.id.btnRealizarToma).setOnClickListener(v ->
                startActivity(new Intent(this, TakeInventoryActivity.class)));

        // ── Shortcuts ─────────────────────────────────────────────────────────
        CardView cardHistorial = findViewById(R.id.cardHistorial);
        CardView cardPendientes = findViewById(R.id.cardPendientes);

        if (cardHistorial != null) {
            cardHistorial.setOnClickListener(v ->
                    startActivity(new Intent(this, InventoryHistoryActivity.class)));
        }
        if (cardPendientes != null) {
            cardPendientes.setOnClickListener(v ->
                    startActivity(new Intent(this, PendingInventoryActivity.class)));
        }

        // ── ViewModel ─────────────────────────────────────────────────────────
        ViewModelFactory factory = new ViewModelFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(HomeViewModel.class);

        viewModel.getHeaderData().observe(this, data -> {
            // Inicial del nombre para el avatar
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
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        viewModel.cargarDatosCabecera();

        // ── Manejo moderno del botón "Atrás" ──────────────────────────────────
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    // Si el menú lateral está abierto, lo cerramos
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    // Si el menú está cerrado, cerramos la actividad y salimos de la app
                    finish();
                }
            }
        });
    }

    // ── Navigation Drawer ────────────────────────────────────────────────────
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_historial) {
            startActivity(new Intent(this, InventoryHistoryActivity.class));

        } else if (id == R.id.nav_pendientes) {
            startActivity(new Intent(this, PendingInventoryActivity.class));

        } else if (id == R.id.nav_toma) {
            startActivity(new Intent(this, TakeInventoryActivity.class));

        } else if (id == R.id.nav_cambiar_ubicacion) {
            Intent intent = new Intent(this, SucursalActivity.class);
            intent.putExtra("CAMBIO_UBICACION", true);
            startActivity(intent);

        } else if (id == R.id.nav_perfil) {
            startActivity(new Intent(this, UserProfileActivity.class));

        } else if (id == R.id.nav_cerrar_sesion) {
            confirmarCerrarSesion();
        }

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

    @Override
    protected void onResume() {
        super.onResume();
        // Recarga los datos de cabecera por si el usuario cambió de ubicación
        viewModel.cargarDatosCabecera();
    }
}