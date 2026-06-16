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
 * Autenticador para la gestión de expiración de Tokens.
 *
 * Se implementa la interfaz Authenticator de OkHttp para interceptar las respuestas 
 * del servidor. Se ejecuta automáticamente al recibir un código HTTP 401 (Unauthorized),
 * indicando que el Access Token ha caducado.
 */
public class TokenAuthenticator implements Authenticator {

    // Se almacena el contexto para la navegación y acceso al almacenamiento local.
    private final Context context;
    // Referencia a SharedPreferences para acceder y persistir los tokens de sesión.
    private final SharedPreferences prefs;

    /**
     * Constructor.
     * Se inicializan las dependencias requeridas para el manejo de sesión.
     */
    public TokenAuthenticator(Context context) {
        this.context = context;
        // Se instancia el acceso al archivo de preferencias en modo privado.
        this.prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
    }

    /**
     * Método de intercepción principal.
     * Se captura la petición fallida, se ejecuta el proceso de renovación de token de forma 
     * bloqueante y se reintenta la petición original con las nuevas credenciales.
     */
    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {

        // 1. VALIDACIÓN DE ESTADO:
        // Se verifica que el código de error sea estrictamente 401.
        // También se comprueba que la petición fallida no sea la propia llamada a "/refresh-token".
        // Esto evita un bucle infinito si el servidor rechaza el Refresh Token.
        if (response.code() != 401 || response.request().url().encodedPath().contains("/refresh-token")) {
            return null;
        }

        // 2. RECUPERACIÓN DE CREDENCIALES:
        // Se obtiene el Refresh Token almacenado localmente.
        String refreshToken = prefs.getString("REFRESH_TOKEN", null);

        // Si no existe un Refresh Token válido, el usuario debe volver a autenticarse.
        if (refreshToken == null || refreshToken.isEmpty()) {
            logoutAndNavigateToLogin();
            return null;
        }

        // 3. RENOVACIÓN DE TOKENS:
        // Se ejecuta la petición de renovación.
        String newAccessToken = fetchNewToken(refreshToken);

        // Se verifica el resultado de la renovación.
        if (newAccessToken != null) {
            // Se reconstruye el Request original modificando únicamente el header de autorización
            // con el nuevo Access Token obtenido.
            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + newAccessToken)
                    .build();
        } else {
            // Si la renovación falla (ej. Refresh Token caducado o inválido),
            // se limpian los datos de sesión y se fuerza la navegación al Login.
            logoutAndNavigateToLogin();
            return null;
        }
    }

    /**
     * Ejecuta una petición HTTP síncrona al endpoint de renovación de tokens.
     */
    private String fetchNewToken(String refreshToken) {
        try {
            // Se instancia un cliente OkHttpClient independiente para esta operación.
            OkHttpClient client = new OkHttpClient();
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");

            // Se construye el payload con el formato requerido por la API.
            String jsonBody = "{\"refreshToken\":\"" + refreshToken + "\"}";
            RequestBody body = RequestBody.create(jsonBody, JSON);

            // Se genera el Request apuntando al endpoint de refresco.
            Request request = new Request.Builder()
                    .url(ApiClient.BASE_URL + "/api/auth/refresh-token")
                    .post(body)
                    .build();

            // Se ejecuta la llamada de forma síncrona mediante .execute().
            // Esto bloquea el hilo de OkHttp hasta recibir la respuesta.
            Response refreshResponse = client.newCall(request).execute();

            // Se valida que el código HTTP sea de la familia 200 y que exista un body.
            if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                String responseString = refreshResponse.body().string();
                JSONObject jsonObject = new JSONObject(responseString);

                // Se verifica la estructura de la respuesta JSON.
                if (jsonObject.has("accessToken") && jsonObject.has("refreshToken")) {
                    String newAccessToken = jsonObject.getString("accessToken");
                    String newRefreshToken = jsonObject.getString("refreshToken");

                    // Se actualizan ambos tokens en SharedPreferences de forma persistente.
                    prefs.edit()
                            .putString("ACCESS_TOKEN", newAccessToken)
                            .putString("REFRESH_TOKEN", newRefreshToken)
                            .apply();

                    return newAccessToken;
                }
            }
        } catch (Exception e) {
            // Se registra cualquier excepción (ej. TimeOut, JSONException) en el Logcat.
            Log.e("TokenAuthenticator", "Error en la petición de refresco de token", e);
        }
        return null;
    }

    /**
     * Limpia los datos de sesión locales y redirige a la pantalla principal.
     */
    private void logoutAndNavigateToLogin() {
        // Se borran todas las llaves persistidas en el archivo de preferencias.
        prefs.edit().clear().apply();

        // Se instancia el Intent hacia la clase LoginActivity.
        Intent intent = new Intent(context, LoginActivity.class);
        
        // Se aplican los flags de navegación:
        // - FLAG_ACTIVITY_NEW_TASK: Inicia la actividad en una nueva tarea.
        // - FLAG_ACTIVITY_CLEAR_TASK: Limpia el backstack completo.
        // Esto previene que el usuario regrese a actividades protegidas usando el botón físico "Atrás".
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        // Se ejecuta el Intent para iniciar la actividad.
        context.startActivity(intent);
    }
}