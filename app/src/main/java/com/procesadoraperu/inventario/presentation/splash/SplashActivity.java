package com.procesadoraperu.inventario.presentation.splash;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.presentation.auth.LoginActivity;
import com.procesadoraperu.inventario.presentation.home.HomeActivity;
import com.procesadoraperu.inventario.presentation.selection.SucursalActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long DURACION_SPLASH_MS = 2200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 1. Vinculamos el nuevo ImageView de tu logo
        ImageView logo = findViewById(R.id.ivLogoAnimado);

        if (logo != null) {
            // Efecto de aparición suave (Fade in)
            logo.setAlpha(0f);
            logo.animate().alpha(1f).setDuration(500).start();

            // Animación de Latido (Escalar de 100% a 115% y regresar a 100%)
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 1f, 1.15f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 1f, 1.15f, 1f);

            // Configuramos para que se repita infinitamente y dure 1.2 segundos cada latido
            scaleX.setRepeatCount(ValueAnimator.INFINITE);
            scaleY.setRepeatCount(ValueAnimator.INFINITE);
            scaleX.setDuration(1200);
            scaleY.setDuration(1200);

            // Iniciamos la animación
            scaleX.start();
            scaleY.start();
        }

        new Handler(Looper.getMainLooper()).postDelayed(this::navegar, DURACION_SPLASH_MS);
    }

    private void navegar() {
        SharedPreferences authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
        SharedPreferences appPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        String accessToken = authPrefs.getString("ACCESS_TOKEN", null);
        String idAlmacen = appPrefs.getString("ACTIVE_ALMACEN_ID", null);

        Intent intent;
        if (accessToken != null) {
            intent = (idAlmacen != null)
                    ? new Intent(this, HomeActivity.class)
                    : new Intent(this, SucursalActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}