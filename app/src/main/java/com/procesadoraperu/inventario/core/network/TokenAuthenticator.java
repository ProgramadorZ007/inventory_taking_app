package com.procesadoraperu.inventario.core.network;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.procesadoraperu.inventario.presentation.auth.LoginActivity;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

public class TokenAuthenticator implements Authenticator {

    private final Context context;
    private final SharedPreferences prefs;

    public TokenAuthenticator(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
        // Si el código NO es 401, no hacemos nada.
        // Si la petición original que falló ya era un intento de refresh, evitamos un bucle infinito.
        if (response.code() != 401 || response.request().url().encodedPath().contains("/refresh-token")) {
            return null;
        }

        String refreshToken = prefs.getString("REFRESH_TOKEN", null);

        // Si no hay refresh token, el usuario debe iniciar sesión manualmente
        if (refreshToken == null || refreshToken.isEmpty()) {
            logoutAndNavigateToLogin();
            return null;
        }

        // 1. Intentamos renovar el token haciendo una llamada síncrona
        String newAccessToken = fetchNewToken(refreshToken);

        if (newAccessToken != null) {
            // 2. ¡Éxito! Clonamos la petición original que falló, pero le ponemos el NUEVO token
            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + newAccessToken)
                    .build();
        } else {
            // 3. Falló la renovación (el refresh token expiró o fue revocado)
            logoutAndNavigateToLogin();
            return null;
        }
    }

    /**
     * Hace la llamada directa a la API de Nisira para refrescar el token.
     */
    private String fetchNewToken(String refreshToken) {
        try {
            OkHttpClient client = new OkHttpClient();
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");

            // Armamos el JSON manualmente
            String jsonBody = "{\"refreshToken\":\"" + refreshToken + "\"}";
            RequestBody body = RequestBody.create(jsonBody, JSON);

            Request request = new Request.Builder()
                    .url(ApiClient.BASE_URL + "/api/auth/refresh-token")
                    .post(body)
                    .build();

            // Ejecutamos la petición
            Response refreshResponse = client.newCall(request).execute();

            // MEJORA: Adaptado al formato real de la API Nisira (sin la envoltura "success")
            if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                String responseString = refreshResponse.body().string();
                JSONObject jsonObject = new JSONObject(responseString);

                // Comprobamos si el JSON tiene directamente el accessToken (Igual que el Login)
                if (jsonObject.has("accessToken") && jsonObject.has("refreshToken")) {
                    String newAccessToken = jsonObject.getString("accessToken");
                    String newRefreshToken = jsonObject.getString("refreshToken");

                    // Guardamos los nuevos tokens en SharedPreferences
                    prefs.edit()
                            .putString("ACCESS_TOKEN", newAccessToken)
                            .putString("REFRESH_TOKEN", newRefreshToken)
                            .apply();

                    return newAccessToken;
                }
            }
        } catch (Exception e) {
            Log.e("TokenAuthenticator", "Error refrescando el token", e);
        }
        return null;
    }

    /**
     * Limpia la sesión y manda al usuario a la pantalla de Login.
     */
    private void logoutAndNavigateToLogin() {
        // Limpiamos los tokens guardados
        prefs.edit().clear().apply();

        // Lanzamos el Activity de Login y limpiamos el historial de pantallas
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}