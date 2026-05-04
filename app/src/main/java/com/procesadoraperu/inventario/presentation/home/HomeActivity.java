package com.procesadoraperu.inventario.presentation.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.presentation.ViewModelFactory;
import com.procesadoraperu.inventario.presentation.auth.LoginActivity;

public class HomeActivity extends AppCompatActivity {

    private HomeViewModel viewModel;
    private TextView tvCurrentLocation;
    // Asumiendo que luego pondrás un botón en tu XML para cerrar sesión
    // private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvCurrentLocation = findViewById(R.id.tvCurrentLocation);
        // btnLogout = findViewById(R.id.btnLogout);

        // Instanciar el ViewModel
        ViewModelFactory factory = new ViewModelFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(HomeViewModel.class);

        // Observadores
        viewModel.getHeaderInfo().observe(this, infoText -> {
            // Actualiza el texto debajo de "Bienvenido"
            tvCurrentLocation.setText(infoText);
        });

        viewModel.getLogoutSuccess().observe(this, success -> {
            if (success) {
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        // Acciones Iniciales: Cargar los datos guardados en SharedPreferences
        viewModel.cargarDatosCabecera();

        // btnLogout.setOnClickListener(v -> viewModel.cerrarSesion());
    }
}