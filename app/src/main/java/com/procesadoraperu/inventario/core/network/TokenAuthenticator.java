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

/**
 * Autenticador inteligente para la gestión de expiración de Tokens.
 *
 * Esta clase implementa la interfaz {@link Authenticator} de OkHttp. Se dispara
 * automáticamente cuando el servidor responde con un error HTTP 401 (Unauthorized).
 * Su función es intentar renovar el Access Token de forma transparente para el usuario.
 */
public class TokenAuthenticator implements Authenticator {

    private final Context context;
    private final SharedPreferences prefs;

    /**
     * Constructor del autenticador.
     *
     * @param context Contexto necesario para manejar SharedPreferences y navegación.
     */
    public TokenAuthenticator(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
    }

    /**
     * Método de intercepción de errores de autenticación.
     *
     * Si la petición original falla por token expirado, este método detiene el flujo,
     * solicita un nuevo token al servidor y reintenta la petición original con
     * las nuevas credenciales.
     */
    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {

        // 1. VALIDACIÓN DE DISPARO:
        // Solo actuamos si el error es 401.
        // Evitamos bucles infinitos si la propia petición de refresco falla.
        if (response.code() != 401 || response.request().url().encodedPath().contains("/refresh-token")) {
            return null;
        }

        // 2. RECUPERACIÓN DEL REFRESH TOKEN:
        String refreshToken = prefs.getString("REFRESH_TOKEN", null);

        if (refreshToken == null || refreshToken.isEmpty()) {
            logoutAndNavigateToLogin();
            return null;
        }

        // 3. RENOVACIÓN SÍNCRONA:
        // Se realiza una llamada bloqueante al servidor para obtener un nuevo par de tokens.
        String newAccessToken = fetchNewToken(refreshToken);

        if (newAccessToken != null) {
            // ÉXITO: Se reconstruye la petición que falló originalmente inyectando el nuevo token.
            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + newAccessToken)
                    .build();
        } else {
            // FALLO CRÍTICO: El Refresh Token ya no es válido, se requiere login manual.
            logoutAndNavigateToLogin();
            return null;
        }
    }

    /**
     * Realiza la petición técnica al endpoint de renovación de Nisira.
     *
     * @param refreshToken El token de larga duración almacenado en el dispositivo.
     * @return El nuevo Access Token en caso de éxito; null en caso contrario.
     */
    private String fetchNewToken(String refreshToken) {
        try {
            OkHttpClient client = new OkHttpClient();
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");

            // Construcción del cuerpo de la petición según el esquema esperado por la API
            String jsonBody = "{\"refreshToken\":\"" + refreshToken + "\"}";
            RequestBody body = RequestBody.create(jsonBody, JSON);

            Request request = new Request.Builder()
                    .url(ApiClient.BASE_URL + "/api/auth/refresh-token")
                    .post(body)
                    .build();

            // Ejecución síncrona de la llamada
            Response refreshResponse = client.newCall(request).execute();

            if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                String responseString = refreshResponse.body().string();
                JSONObject jsonObject = new JSONObject(responseString);

                // Procesamiento de la respuesta (Formato estándar de API Nisira)
                if (jsonObject.has("accessToken") && jsonObject.has("refreshToken")) {
                    String newAccessToken = jsonObject.getString("accessToken");
                    String newRefreshToken = jsonObject.getString("refreshToken");

                    // Persistencia de las nuevas credenciales
                    prefs.edit()
                            .putString("ACCESS_TOKEN", newAccessToken)
                            .putString("REFRESH_TOKEN", newRefreshToken)
                            .apply();

                    return newAccessToken;
                }
            }
        } catch (Exception e) {
            Log.e("TokenAuthenticator", "Error crítico en proceso de refresco de token", e);
        }
        return null;
    }

    /**
     * Realiza el cierre de sesión forzoso y redirige al usuario a la pantalla de entrada.
     *
     * Este método garantiza que, si la seguridad del usuario se ve comprometida
     * o la sesión expira totalmente, la app regrese a un estado seguro.
     */
    private void logoutAndNavigateToLogin() {
        // Limpieza de caché de credenciales
        prefs.edit().clear().apply();

        // Navegación con limpieza de historial (Evita que el usuario regrese con el botón atrás)
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}