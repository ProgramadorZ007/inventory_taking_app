package com.procesadoraperu.inventario.presentation.splash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.presentation.auth.LoginActivity;
import com.procesadoraperu.inventario.presentation.home.HomeActivity;
import com.procesadoraperu.inventario.presentation.selection.SucursalActivity;

/**
 * SplashActivity — Pantalla de presentación con animación Lottie.
 *
 * Flujo:
 * 1. Muestra la animación (~2.5 seg).
 * 2. Verifica si existe una sesión activa en SharedPreferences.
 * 3. Si hay sesión y ubicación seleccionada → HomeActivity.
 * Si hay sesión pero sin ubicación → SucursalActivity.
 * Sin sesión → LoginActivity.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long DURACION_SPLASH_MS = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Una vez sincronizado el Gradle, este error desaparecerá por completo
        LottieAnimationView lottie = findViewById(R.id.lottieView);
        if (lottie != null) {
            lottie.playAnimation();
        }

        new Handler(Looper.getMainLooper()).postDelayed(this::navegar, DURACION_SPLASH_MS);
    }

    private void navegar() {
        SharedPreferences authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
        // NOTA DE ARQUITECTURA: Asegúrate de que AlmacenRepositoryImpl use este mismo nombre de archivo "app_prefs"
        SharedPreferences appPrefs  = getSharedPreferences("app_prefs",  MODE_PRIVATE);

        String accessToken = authPrefs.getString("ACCESS_TOKEN", null);
        String idAlmacen   = appPrefs.getString("ACTIVE_ALMACEN_ID", null);

        Intent intent;
        if (accessToken != null) {
            // Sesión activa: saltar el login
            intent = (idAlmacen != null)
                    ? new Intent(this, HomeActivity.class)       // ya tiene ubicación
                    : new Intent(this, SucursalActivity.class);  // falta elegir ubicación
        } else {
            intent = new Intent(this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
    }
}