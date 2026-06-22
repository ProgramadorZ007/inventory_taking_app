package com.procesadoraperu.inventario.presentation.auth;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.core.sync.SyncScheduler;
import com.procesadoraperu.inventario.presentation.ViewModelFactory;
import com.procesadoraperu.inventario.presentation.home.HomeActivity;
import com.procesadoraperu.inventario.presentation.legal.LegalActivity;
import com.procesadoraperu.inventario.presentation.selection.SucursalActivity;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel viewModel;
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        // Verificar sesión existente antes de inflar la UI
        SharedPreferences authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
        String accessToken = authPrefs.getString("ACCESS_TOKEN", null);
        if (accessToken != null) {
            // Verificar si el token aún es válido o si hay refresh token para renovar
            boolean tokenValid = !com.procesadoraperu.inventario.core.utils.JwtDecoder.isTokenExpired(accessToken);
            String refreshToken = authPrefs.getString("REFRESH_TOKEN", null);
            boolean canRefresh = refreshToken != null && !refreshToken.isEmpty();

            if (tokenValid || canRefresh) {
                navegarSiguiente();
                return;
            }
            // Si el token expiró y no hay refresh, limpiar sesión y mostrar login
            authPrefs.edit().clear().apply();
        }

        setContentView(R.layout.activity_login);

        etUsername  = findViewById(R.id.etUsername);
        etPassword  = findViewById(R.id.etPassword);
        btnLogin    = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBarLogin);

        // ✔️ AQUÍ: Conectamos la imagen y le ponemos la animación
        ImageView logo = findViewById(R.id.ivLogoLogin);
        if (logo != null) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 1f, 1.10f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 1f, 1.10f, 1f);

            scaleX.setRepeatCount(ValueAnimator.INFINITE);
            scaleY.setRepeatCount(ValueAnimator.INFINITE);
            scaleX.setDuration(1500); // Un poco más lento que el Splash para que no desespere
            scaleY.setDuration(1500);

            scaleX.start();
            scaleY.start();
        }

        // Manejo de Insets (Safe Area + teclado)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,
                    Math.max(systemBars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });

        // Listeners legales
        TextView tvTerminos = findViewById(R.id.tvTerminos);
        TextView tvPrivacidad = findViewById(R.id.tvPrivacidad);

        if (tvTerminos != null) {
            tvTerminos.setOnClickListener(v -> {
                Intent intent = new Intent(this, LegalActivity.class);
                intent.putExtra(LegalActivity.EXTRA_TIPO, LegalActivity.TIPO_TERMINOS);
                startActivity(intent);
            });
        }

        if (tvPrivacidad != null) {
            tvPrivacidad.setOnClickListener(v -> {
                Intent intent = new Intent(this, LegalActivity.class);
                intent.putExtra(LegalActivity.EXTRA_TIPO, LegalActivity.TIPO_PRIVACIDAD);
                startActivity(intent);
            });
        }

        setupViewModel();
        setupListeners();
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(LoginViewModel.class);

        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnLogin.setEnabled(!isLoading);
            btnLogin.setText(isLoading ? "Verificando..." : "INGRESAR");
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Snackbar.make(findViewById(android.R.id.content), error, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getColor(R.color.pp_error))
                        .show();
            }
        });

        viewModel.getLoginSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) navegarSiguiente();
        });
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> {
            String user = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            if (user.isEmpty() || pass.isEmpty()) {
                Snackbar.make(v, "Completa todos los campos", Snackbar.LENGTH_SHORT).show();
                return;
            }
            viewModel.login(user, pass);
        });
    }

    private void navegarSiguiente() {
        // Al iniciar sesión, programar sincronización de pendientes (si los hay)
        SyncScheduler.scheduleOnce(this);

        SharedPreferences appPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String idAlmacen = appPrefs.getString("ACTIVE_ALMACEN_ID", null);
        Intent intent = (idAlmacen != null)
                ? new Intent(this, HomeActivity.class)
                : new Intent(this, SucursalActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}