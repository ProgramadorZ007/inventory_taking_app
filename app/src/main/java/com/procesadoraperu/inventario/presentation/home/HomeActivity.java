package com.procesadoraperu.inventario.presentation.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.domain.model.sucursal.Sucursal;
import com.procesadoraperu.inventario.presentation.ViewModelFactory;
import com.procesadoraperu.inventario.presentation.auth.LoginActivity;

public class HomeActivity extends AppCompatActivity {

    private HomeViewModel viewModel;
    private Spinner spinnerSucursales;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        spinnerSucursales = findViewById(R.id.spinnerSucursales);
        btnLogout = findViewById(R.id.btnLogout);

        // Instanciar el ViewModel usando nuestra Fábrica
        ViewModelFactory factory = new ViewModelFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(HomeViewModel.class);

        // Observadores
        viewModel.getSucursales().observe(this, sucursales -> {
            if (sucursales != null && !sucursales.isEmpty()) {
                ArrayAdapter<Sucursal> adapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_dropdown_item, sucursales);
                spinnerSucursales.setAdapter(adapter);
            }
        });

        viewModel.getLogoutSuccess().observe(this, success -> {
            if (success) {
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });

        // Acciones Iniciales
        viewModel.cargarSucursales();

        btnLogout.setOnClickListener(v -> viewModel.cerrarSesion());
    }
}