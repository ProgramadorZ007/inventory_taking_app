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
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.HttpMetric;


public class ApiClient {

    public static final String BASE_URL = "https://api.procesadoraperu.com";

    // CORRECCIÓN: volatile para thread-safety
    private static volatile Retrofit retrofit = null;

    /**
     * CORRECCIÓN: Resetea el singleton. Llamar desde AuthRepositoryImpl.logout()
     * para que el siguiente login construya un cliente HTTP limpio.
     */
    public static synchronized void reset() {
        retrofit = null;
    }

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            synchronized (ApiClient.class) {
                if (retrofit == null) {

                    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

                    Interceptor authInterceptor = new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request originalRequest = chain.request();

                            String urlPath = originalRequest.url().encodedPath();
                            if (urlPath.contains("/auth/login") || urlPath.contains("/auth/refresh-token")) {
                                return chain.proceed(originalRequest);
                            }

                            SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
                            String token = prefs.getString("ACCESS_TOKEN", null);

                            Request.Builder requestBuilder = originalRequest.newBuilder();

                            if (token != null && !token.isEmpty()) {
                                requestBuilder.header("Authorization", "Bearer " + token);
                                requestBuilder.header("Content-Type", "application/json");
                            }

                            return chain.proceed(requestBuilder.build());
                        }
                    };

                    Interceptor firebasePerfInterceptor = chain -> {
                        Request request = chain.request();
                        HttpMetric metric = FirebasePerformance.getInstance()
                                .newHttpMetric(request.url().toString(), request.method());
                        metric.start();
                        Response response = null;
                        try {
                            response = chain.proceed(request);
                            metric.setHttpResponseCode(response.code());
                            metric.setResponseContentType(response.header("Content-Type"));
                            if (response.body() != null) {
                                metric.setResponsePayloadSize(response.body().contentLength());
                            }
                        } finally {
                            metric.stop();
                        }
                        return response;
                    };

                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .addInterceptor(loggingInterceptor)
                            .addInterceptor(authInterceptor)
                            .addInterceptor(firebasePerfInterceptor)
                            .authenticator(new TokenAuthenticator(context))
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .build();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(okHttpClient)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return retrofit;
    }
}