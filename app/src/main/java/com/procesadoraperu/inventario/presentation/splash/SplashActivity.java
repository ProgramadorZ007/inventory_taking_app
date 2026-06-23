package com.procesadoraperu.inventario.presentation.splash;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.presentation.auth.LoginActivity;
import com.procesadoraperu.inventario.presentation.home.HomeActivity;
import com.procesadoraperu.inventario.presentation.selection.SucursalActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long DURACION_SPLASH_MS = 3200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Instalar el splash screen del sistema e inmediatamente descartarlo
        // para que tu animación personalizada sea lo único visible
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> false);

        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        iniciarAnimaciones();

        new Handler(Looper.getMainLooper()).postDelayed(this::navegar, DURACION_SPLASH_MS);
    }

    private void iniciarAnimaciones() {
        View logo = findViewById(R.id.ivLogoSplash);
        View brand = findViewById(R.id.tvBrand);
        View date = findViewById(R.id.llDate);
        View leaf = findViewById(R.id.ivLeafCenter);
        View fullName = findViewById(R.id.tvFullName);
        View slogan = findViewById(R.id.tvSlogan);
        View line = findViewById(R.id.vLineAccent);
        View dotsTop = findViewById(R.id.ivDotsTop);
        View dotsBottom = findViewById(R.id.ivDotsBottom);
        View version = findViewById(R.id.llVersionInfo);

        if (logo == null) return;

        // Configuración inicial (invisible/desplazado)
        float shift = 60f;
        logo.setAlpha(0f); logo.setTranslationY(shift);
        brand.setAlpha(0f); brand.setTranslationY(shift);
        date.setAlpha(0f);
        leaf.setAlpha(0f); leaf.setScaleX(0f); leaf.setScaleY(0f);
        fullName.setAlpha(0f); fullName.setTranslationY(shift);
        slogan.setAlpha(0f); slogan.setTranslationY(shift);
        line.setAlpha(0f); line.setScaleX(0f);
        version.setAlpha(0f);
        dotsTop.setAlpha(0f);
        dotsBottom.setAlpha(0f);

        // Animación de Logo (con rebote)
        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(logo, View.ALPHA, 0f, 1f);
        ObjectAnimator logoTrans = ObjectAnimator.ofFloat(logo, View.TRANSLATION_Y, shift, 0f);
        logoTrans.setInterpolator(new OvershootInterpolator());

        // Animación de Marca
        ObjectAnimator brandAlpha = ObjectAnimator.ofFloat(brand, View.ALPHA, 0f, 1f);
        ObjectAnimator brandTrans = ObjectAnimator.ofFloat(brand, View.TRANSLATION_Y, shift, 0f);

        // Hoja central
        ObjectAnimator leafAlpha = ObjectAnimator.ofFloat(leaf, View.ALPHA, 0f, 0.7f);
        ObjectAnimator leafScaleX = ObjectAnimator.ofFloat(leaf, View.SCALE_X, 0f, 1f);
        ObjectAnimator leafScaleY = ObjectAnimator.ofFloat(leaf, View.SCALE_Y, 0f, 1f);

        // Grupo de texto secundario
        ObjectAnimator nameAlpha = ObjectAnimator.ofFloat(fullName, View.ALPHA, 0f, 1f);
        ObjectAnimator nameTrans = ObjectAnimator.ofFloat(fullName, View.TRANSLATION_Y, shift, 0f);
        ObjectAnimator sloganAlpha = ObjectAnimator.ofFloat(slogan, View.ALPHA, 0f, 1f);
        ObjectAnimator sloganTrans = ObjectAnimator.ofFloat(slogan, View.TRANSLATION_Y, shift, 0f);

        // Línea naranja expansiva
        ObjectAnimator lineAlpha = ObjectAnimator.ofFloat(line, View.ALPHA, 0f, 1f);
        ObjectAnimator lineScale = ObjectAnimator.ofFloat(line, View.SCALE_X, 0f, 1f);

        // Secuenciador
        AnimatorSet logoSet = new AnimatorSet();
        logoSet.playTogether(logoAlpha, logoTrans);
        logoSet.setDuration(1000);

        AnimatorSet brandSet = new AnimatorSet();
        brandSet.playTogether(brandAlpha, brandTrans);
        brandSet.setDuration(800);
        brandSet.setStartDelay(300);

        AnimatorSet leafSet = new AnimatorSet();
        leafSet.playTogether(leafAlpha, leafScaleX, leafScaleY);
        leafSet.setDuration(600);
        leafSet.setStartDelay(600);

        AnimatorSet infoSet = new AnimatorSet();
        infoSet.playTogether(nameAlpha, nameTrans, sloganAlpha, sloganTrans);
        infoSet.setDuration(800);
        infoSet.setStartDelay(800);

        AnimatorSet accentSet = new AnimatorSet();
        accentSet.playTogether(lineAlpha, lineScale);
        accentSet.setDuration(600);
        accentSet.setStartDelay(1200);

        // Iniciar todo
        logoSet.start();
        brandSet.start();
        leafSet.start();
        infoSet.start();
        accentSet.start();

        // Elementos decorativos aparecen suavemente
        date.animate().alpha(0.8f).setDuration(1000).setStartDelay(500).start();
        dotsTop.animate().alpha(0.4f).setDuration(1500).setStartDelay(200).start();
        dotsBottom.animate().alpha(0.3f).setDuration(1500).setStartDelay(400).start();
        version.animate().alpha(1f).setDuration(1000).setStartDelay(1500).start();
    }

    private void navegar() {
        SharedPreferences authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
        SharedPreferences appPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        String accessToken = authPrefs.getString("ACCESS_TOKEN", null);
        String idAlmacen = appPrefs.getString("ACTIVE_ALMACEN_ID", null);

        Intent intent;
        if (accessToken != null) {
            // Verificar si el token es válido o si hay refresh token para renovar
            boolean tokenValid = !com.procesadoraperu.inventario.core.utils.JwtDecoder.isTokenExpired(accessToken);
            String refreshToken = authPrefs.getString("REFRESH_TOKEN", null);
            boolean canRefresh = refreshToken != null && !refreshToken.isEmpty();

            if (tokenValid || canRefresh) {
                intent = (idAlmacen != null)
                        ? new Intent(this, HomeActivity.class)
                        : new Intent(this, SucursalActivity.class);
            } else {
                // Token expirado y sin refresh → limpiar y enviar a login
                authPrefs.edit().clear().apply();
                intent = new Intent(this, LoginActivity.class);
            }
        } else {
            intent = new Intent(this, LoginActivity.class);
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}