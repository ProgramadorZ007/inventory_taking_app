package com.procesadoraperu.inventario.presentation.splash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
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

        LottieAnimationView lottie = findViewById(R.id.lottieView);
        if (lottie != null) {
            lottie.setAlpha(0f);
            lottie.animate().alpha(1f).setDuration(500).start();
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