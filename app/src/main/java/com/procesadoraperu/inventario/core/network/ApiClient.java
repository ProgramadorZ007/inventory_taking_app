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

public class ApiClient {

    // Dominio real de tu servidor Nisira
    public static final String BASE_URL = "https://api.procesadoraperu.com";

    private static Retrofit retrofit = null;

    /**
     * Devuelve la instancia única (Singleton) de Retrofit configurada.
     */
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {

            // 1. Log Interceptor: Muestra en consola (Logcat) el JSON que entra y sale.
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // 2. Auth Interceptor: Inyecta el Token JWT en la cabecera (Header)
            Interceptor authInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request originalRequest = chain.request();

                    // MEJORA: Si la ruta es login O refresh-token, NO le ponemos el token viejo
                    String urlPath = originalRequest.url().encodedPath();
                    if (urlPath.contains("/auth/login") || urlPath.contains("/auth/refresh-token")) {
                        return chain.proceed(originalRequest);
                    }

                    // Para todas las demás peticiones, leemos el token guardado
                    SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
                    String token = prefs.getString("ACCESS_TOKEN", null);

                    Request.Builder requestBuilder = originalRequest.newBuilder();

                    // Si existe el token, agregamos el "Bearer"
                    if (token != null && !token.isEmpty()) {
                        requestBuilder.header("Authorization", "Bearer " + token);
                        requestBuilder.header("Content-Type", "application/json");
                    }

                    return chain.proceed(requestBuilder.build());
                }
            };

            // 3. Ensamblamos el Cliente OkHttp con reglas de "TimeOut" y el Authenticator
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(authInterceptor)
                    .authenticator(new TokenAuthenticator(context)) // <--- MEJORA: LÍNEA AGREGADA
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            // 4. Construimos Retrofit
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create()) // Convierte JSON a Java Automáticamente
                    .build();
        }
        return retrofit;
    }
}