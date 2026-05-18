package com.procesadoraperu.inventario.presentation.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.presentation.ViewModelFactory;
import com.procesadoraperu.inventario.presentation.home.HomeActivity;
import com.procesadoraperu.inventario.presentation.legal.LegalActivity;
import com.procesadoraperu.inventario.presentation.selection.SucursalActivity;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel viewModel;
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ── Verificar sesión activa ANTES de inflar el layout ───────────────
        // Esto evita que el usuario tenga que loguearse cada vez que abre la app.
        SharedPreferences authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
        SharedPreferences appPrefs  = getSharedPreferences("app_prefs",  MODE_PRIVATE);
        String accessToken = authPrefs.getString("ACCESS_TOKEN", null);

        if (accessToken != null) {
            String idAlmacen = appPrefs.getString("ACTIVE_ALMACEN_ID", null);
            Intent dest = (idAlmacen != null)
                    ? new Intent(this, HomeActivity.class)
                    : new Intent(this, SucursalActivity.class);
            startActivity(dest);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        rootView     = findViewById(android.R.id.content);
        etUsername   = findViewById(R.id.etUsername);
        etPassword   = findViewById(R.id.etPassword);
        btnLogin     = findViewById(R.id.btnLogin);
        progressBar  = findViewById(R.id.progressBarLogin);

        // ── Manejo correcto del teclado (ajustar padding en vez de resize) ──
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int navBar    = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(0, 0, 0, Math.max(imeHeight, navBar));
            return insets;
        });

        // ── Términos y Privacidad ────────────────────────────────────────────
        TextView tvTerminos  = findViewById(R.id.tvTerminos);
        TextView tvPrivacidad = findViewById(R.id.tvPrivacidad);

        tvTerminos.setOnClickListener(v -> {
            Intent intent = new Intent(this, LegalActivity.class);
            intent.putExtra(LegalActivity.EXTRA_TIPO, LegalActivity.TIPO_TERMINOS);
            startActivity(intent);
        });

        tvPrivacidad.setOnClickListener(v -> {
            Intent intent = new Intent(this, LegalActivity.class);
            intent.putExtra(LegalActivity.EXTRA_TIPO, LegalActivity.TIPO_PRIVACIDAD);
            startActivity(intent);
        });

        // ── ViewModel ─────────────────────────────────────────────────────────
        ViewModelFactory factory = new ViewModelFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(LoginViewModel.class);

        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnLogin.setEnabled(!isLoading);
            btnLogin.setText(isLoading ? "Verificando…" : "INGRESAR");
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error == null) return;
            Snackbar snack = Snackbar.make(rootView, error, Snackbar.LENGTH_LONG);
            snack.setBackgroundTint(getColor(R.color.pp_error));
            snack.setTextColor(getColor(R.color.pp_white));
            snack.show();
        });

        viewModel.getLoginSuccess().observe(this, isSuccess -> {
            if (Boolean.TRUE.equals(isSuccess)) {
                startActivity(new Intent(this, SucursalActivity.class));
                finish();
            }
        });

        btnLogin.setOnClickListener(v -> {
            String user = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            if (user.isEmpty()) { etUsername.setError("Ingresa tu usuario"); return; }
            if (pass.isEmpty()) { etPassword.setError("Ingresa tu contraseña"); return; }
            viewModel.login(user, pass);
        });
    }
}