package com.procesadoraperu.inventario.core.network;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Cliente centralizado de red para la comunicación con el servidor Nisira.
 *
 * Esta clase implementa el patrón Singleton para garantizar una única instancia de Retrofit.
 * Incluye configuración de seguridad mediante inyección de Tokens JWT, registro de logs
 * para depuración y gestión de tiempos de espera (Timeouts).
 */
public class ApiClient {

    /**
     * URL base del servidor de servicios de Procesadora Perú.
     */
    public static final String BASE_URL = "https://api.procesadoraperu.com";

    private static Retrofit retrofit = null;

    /**
     * Obtiene la instancia configurada de Retrofit.
     *
     * Configura el cliente HTTP con tres capas principales:
     * 1. Monitoreo (Logging Interceptor).
     * 2. Seguridad (Authorization Interceptor).
     * 3. Resiliencia (Token Authenticator).
     *
     * @param context Contexto de la aplicación necesario para acceder a SharedPreferences.
     * @return Instancia única de {@link Retrofit}.
     */
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {

            // =================================================================
            // 1. INTERCEPTOR DE LOGS (DEPURACIÓN)
            // =================================================================
            // Permite visualizar en el Logcat el cuerpo completo (BODY) de las
            // peticiones y respuestas JSON durante el desarrollo.
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // =================================================================
            // 2. INTERCEPTOR DE AUTORIZACIÓN (SEGURIDAD)
            // =================================================================
            // Se encarga de inyectar el Token JWT en el encabezado de cada petición.
            Interceptor authInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request originalRequest = chain.request();

                    // Evitamos inyectar el token en rutas públicas o de renovación
                    String urlPath = originalRequest.url().encodedPath();
                    if (urlPath.contains("/auth/login") || urlPath.contains("/auth/refresh-token")) {
                        return chain.proceed(originalRequest);
                    }

                    // Recuperación del Token almacenado de forma segura
                    SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
                    String token = prefs.getString("ACCESS_TOKEN", null);

                    Request.Builder requestBuilder = originalRequest.newBuilder();

                    // Inyección del header de autorización según el estándar Bearer Token
                    if (token != null && !token.isEmpty()) {
                        requestBuilder.header("Authorization", "Bearer " + token);
                        requestBuilder.header("Content-Type", "application/json");
                    }

                    return chain.proceed(requestBuilder.build());
                }
            };

            // =================================================================
            // 3. CONFIGURACIÓN DEL CLIENTE OKHTTP
            // =================================================================
            // Definimos los tiempos de espera y adjuntamos los interceptores.
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(authInterceptor)
                    // Gestiona automáticamente el error 401 (Unauthorized) renovando el token
                    .authenticator(new TokenAuthenticator(context))
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            // =================================================================
            // 4. CONSTRUCCIÓN DE LA INSTANCIA RETROFIT
            // =================================================================
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    // Motor de conversión automática de JSON a objetos Java
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}