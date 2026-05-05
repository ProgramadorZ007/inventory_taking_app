package com.procesadoraperu.inventario.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.presentation.ViewModelFactory;
import com.procesadoraperu.inventario.presentation.selection.SucursalActivity;

public class UserProfileActivity extends AppCompatActivity {

    private UserProfileViewModel viewModel;

    private TextView tvNombreCompleto, tvUsername, tvCodigo;
    private TextView tvSucursal, tvAlmacen, tvIdAlmacen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mi Perfil");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Vistas
        tvNombreCompleto = findViewById(R.id.tvNombreCompleto);
        tvUsername       = findViewById(R.id.tvUsername);
        tvCodigo         = findViewById(R.id.tvCodigo);
        tvSucursal       = findViewById(R.id.tvSucursal);
        tvAlmacen        = findViewById(R.id.tvAlmacen);
        tvIdAlmacen      = findViewById(R.id.tvIdAlmacen);

        // Botón cambiar ubicación
        findViewById(R.id.btnCambiarUbicacion).setOnClickListener(v -> {
            Intent intent = new Intent(this, SucursalActivity.class);
            intent.putExtra("CAMBIO_UBICACION", true);
            startActivity(intent);
            finish();
        });

        // ViewModel
        ViewModelFactory factory = new ViewModelFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(UserProfileViewModel.class);

        viewModel.getProfileData().observe(this, data -> {
            tvNombreCompleto.setText(data.nombres);
            tvUsername.setText("@" + data.username);
            tvCodigo.setText(data.idCodigoGeneral);
            tvSucursal.setText(data.sucursal);
            tvAlmacen.setText(data.almacen);
            tvIdAlmacen.setText("Cód: " + data.idAlmacen);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });

        viewModel.cargarPerfil();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}