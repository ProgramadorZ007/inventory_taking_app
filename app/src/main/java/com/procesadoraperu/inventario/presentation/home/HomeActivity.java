package com.procesadoraperu.inventario.presentation.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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

    // Header views (nav drawer header)
    private TextView tvNavNombre, tvNavSucursal, tvNavAlmacen;
    // Toolbar subtitle
    private TextView tvToolbarSucursal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // --- Toolbar ---
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }
        tvToolbarSucursal = findViewById(R.id.tvToolbarSubtitle);

        // --- Drawer ---
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
        tvNavNombre   = headerView.findViewById(R.id.tvNavNombre);
        tvNavSucursal = headerView.findViewById(R.id.tvNavSucursal);
        tvNavAlmacen  = headerView.findViewById(R.id.tvNavAlmacen);

        // Botón principal "Realizar Toma"
        findViewById(R.id.btnRealizarToma).setOnClickListener(v ->
                startActivity(new Intent(this, TakeInventoryActivity.class)));

        // ViewModel
        ViewModelFactory factory = new ViewModelFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(HomeViewModel.class);

        viewModel.getHeaderData().observe(this, data -> {
            tvNavNombre.setText(data.nombreUsuario);
            tvNavSucursal.setText(data.sucursal);
            tvNavAlmacen.setText(data.almacen);
            tvToolbarSucursal.setText(data.sucursal + " · " + data.almacen);
        });

        viewModel.getLogoutSuccess().observe(this, success -> {
            if (success) {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        viewModel.cargarDatosCabecera();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_historial) {
            startActivity(new Intent(this, InventoryHistoryActivity.class));

        } else if (id == R.id.nav_pendientes) {
            startActivity(new Intent(this, PendingInventoryActivity.class));

        } else if (id == R.id.nav_cambiar_ubicacion) {
            Intent intent = new Intent(this, SucursalActivity.class);
            // Flag para que al terminar vuelva al Home con datos frescos
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
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Sí, cerrar", (dialog, which) -> viewModel.cerrarSesion())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar cabecera por si cambió la ubicación
        viewModel.cargarDatosCabecera();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // Evitar retroceder al login
            // No llamamos a super para bloquear el botón atrás en Home
        }
    }
}