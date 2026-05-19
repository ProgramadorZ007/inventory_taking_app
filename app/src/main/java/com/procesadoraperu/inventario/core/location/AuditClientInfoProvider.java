package com.procesadoraperu.inventario.core.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.procesadoraperu.inventario.domain.model.inventario.AuditClientInfo;
import com.procesadoraperu.inventario.domain.provider.IAuditClientInfoProvider;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Proveedor de información de auditoría del cliente.
 *
 * Estrategia GPS (3 intentos en cascada):
 *   1. getCurrentLocation(HIGH_ACCURACY)  — ubicación fresca, timeout 8 seg
 *   2. getLastLocation()                  — última conocida en caché
 *   3. requestLocationUpdates()           — fuerza al proveedor a escanear, timeout 12 seg
 * Si los 3 fallan, se registra con lat/lon vacíos (nunca se bloquea el flujo).
 */
public class AuditClientInfoProvider implements IAuditClientInfoProvider {

    private static final String TAG = "AuditClientInfoProvider";

    // Tiempo máximo esperando GPS antes de rendirse (milisegundos)
    private static final long GPS_TIMEOUT_MS        = 8_000L;
    private static final long GPS_FALLBACK_TIMEOUT  = 12_000L;

    private final FusedLocationProviderClient fusedLocationClient;
    private final Context context;

    public AuditClientInfoProvider(Context context) {
        this.context = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context);
    }

    @Override
    @SuppressLint("MissingPermission")
    public void getAuditInfo(OnAuditInfoCallback callback) {

        // ── Datos del hardware (siempre disponibles) ────────────────────────
        final String dispositivo = Build.MANUFACTURER + " " + Build.MODEL;
        final String hostname    = Build.DEVICE;
        final String ip          = getLocalIpAddress();
        final String userAgent   = obtenerUserAgent();

        // ── Verificar permisos antes de intentar GPS ─────────────────────────
        boolean tienePermiso =
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED
                        ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED;

        if (!tienePermiso) {
            Log.w(TAG, "Sin permiso de ubicación — registrando sin coordenadas");
            callback.onSuccess(new AuditClientInfo(dispositivo, ip, hostname, userAgent, "", ""));
            return;
        }

        // ── Intento 1: getCurrentLocation con timeout manual ─────────────────
        intentoGetCurrentLocation(dispositivo, ip, hostname, userAgent, callback);
    }

    /**
     * Intento 1: getCurrentLocation (ubicación fresca del GPS/Red).
     * Aplica un timeout manual de GPS_TIMEOUT_MS para no bloquear el flujo.
     */
    @SuppressLint("MissingPermission")
    private void intentoGetCurrentLocation(String dispositivo, String ip, String hostname,
                                           String userAgent, OnAuditInfoCallback callback) {

        // AtomicBoolean para evitar que el callback se llame dos veces
        // (una vez por el resultado y otra por el timeout)
        final AtomicBoolean respondido = new AtomicBoolean(false);
        final Handler handler = new Handler(Looper.getMainLooper());
        final CancellationTokenSource cts = new CancellationTokenSource();

        // Timeout: si en GPS_TIMEOUT_MS no llegó respuesta, pasamos al fallback
        Runnable timeoutRunnable = () -> {
            if (respondido.compareAndSet(false, true)) {
                Log.w(TAG, "getCurrentLocation timeout — pasando a getLastLocation");
                cts.cancel(); // Cancela la tarea de GPS activa
                intentoGetLastLocation(dispositivo, ip, hostname, userAgent, callback);
            }
        };
        handler.postDelayed(timeoutRunnable, GPS_TIMEOUT_MS);

        try {
            fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                    .addOnSuccessListener(location -> {
                        handler.removeCallbacks(timeoutRunnable);
                        if (respondido.compareAndSet(false, true)) {
                            if (location != null) {
                                Log.d(TAG, "GPS OK: " + location.getLatitude() + ", " + location.getLongitude());
                                callback.onSuccess(new AuditClientInfo(
                                        dispositivo, ip, hostname, userAgent,
                                        String.valueOf(location.getLatitude()),
                                        String.valueOf(location.getLongitude())
                                ));
                            } else {
                                Log.w(TAG, "getCurrentLocation devolvió null — pasando a getLastLocation");
                                intentoGetLastLocation(dispositivo, ip, hostname, userAgent, callback);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        handler.removeCallbacks(timeoutRunnable);
                        if (respondido.compareAndSet(false, true)) {
                            Log.w(TAG, "getCurrentLocation falló: " + e.getMessage());
                            intentoGetLastLocation(dispositivo, ip, hostname, userAgent, callback);
                        }
                    });

        } catch (Exception e) {
            handler.removeCallbacks(timeoutRunnable);
            if (respondido.compareAndSet(false, true)) {
                Log.e(TAG, "Excepción en getCurrentLocation: " + e.getMessage());
                intentoGetLastLocation(dispositivo, ip, hostname, userAgent, callback);
            }
        }
    }

    /**
     * Intento 2: getLastLocation — última coordenada en caché del proveedor.
     * Rápido pero puede ser null si el GPS no se usó recientemente.
     */
    @SuppressLint("MissingPermission")
    private void intentoGetLastLocation(String dispositivo, String ip, String hostname,
                                        String userAgent, OnAuditInfoCallback callback) {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            Log.d(TAG, "LastLocation OK: " + location.getLatitude() + ", " + location.getLongitude());
                            callback.onSuccess(new AuditClientInfo(
                                    dispositivo, ip, hostname, userAgent,
                                    String.valueOf(location.getLatitude()),
                                    String.valueOf(location.getLongitude())
                            ));
                        } else {
                            // getLastLocation también vino null → forzar escaneo activo
                            Log.w(TAG, "getLastLocation null — intentando requestLocationUpdates");
                            intentoRequestUpdates(dispositivo, ip, hostname, userAgent, callback);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "getLastLocation falló: " + e.getMessage());
                        intentoRequestUpdates(dispositivo, ip, hostname, userAgent, callback);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Excepción en getLastLocation: " + e.getMessage());
            intentoRequestUpdates(dispositivo, ip, hostname, userAgent, callback);
        }
    }

    /**
     * Intento 3: requestLocationUpdates — fuerza al hardware GPS/Red a escanear.
     * Más agresivo, útil cuando el caché está vacío (primera vez del día).
     * Aplica timeout de GPS_FALLBACK_TIMEOUT.
     */
    @SuppressLint("MissingPermission")
    private void intentoRequestUpdates(String dispositivo, String ip, String hostname,
                                       String userAgent, OnAuditInfoCallback callback) {

        final AtomicBoolean respondido = new AtomicBoolean(false);
        final Handler handler = new Handler(Looper.getMainLooper());

        // Configuración: una sola actualización, máxima precisión
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdates(1)
                .setWaitForAccurateLocation(false) // No esperar GPS perfecto
                .build();

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                handler.removeCallbacksAndMessages(null);
                if (respondido.compareAndSet(false, true)) {
                    fusedLocationClient.removeLocationUpdates(this);
                    if (locationResult != null && locationResult.getLastLocation() != null) {
                        android.location.Location loc = locationResult.getLastLocation();
                        Log.d(TAG, "RequestUpdates OK: " + loc.getLatitude() + ", " + loc.getLongitude());
                        callback.onSuccess(new AuditClientInfo(
                                dispositivo, ip, hostname, userAgent,
                                String.valueOf(loc.getLatitude()),
                                String.valueOf(loc.getLongitude())
                        ));
                    } else {
                        Log.w(TAG, "RequestUpdates sin resultado — registrando sin coordenadas");
                        callback.onSuccess(new AuditClientInfo(dispositivo, ip, hostname, userAgent, "", ""));
                    }
                }
            }
        };

        // Timeout final: si en GPS_FALLBACK_TIMEOUT no hay señal, rendirse
        handler.postDelayed(() -> {
            if (respondido.compareAndSet(false, true)) {
                try { fusedLocationClient.removeLocationUpdates(locationCallback); } catch (Exception ignored) {}
                Log.w(TAG, "RequestUpdates timeout — registrando sin coordenadas");
                callback.onSuccess(new AuditClientInfo(dispositivo, ip, hostname, userAgent, "", ""));
            }
        }, GPS_FALLBACK_TIMEOUT);

        try {
            fusedLocationClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
            );
        } catch (Exception e) {
            handler.removeCallbacksAndMessages(null);
            if (respondido.compareAndSet(false, true)) {
                Log.e(TAG, "Excepción en requestLocationUpdates: " + e.getMessage());
                callback.onSuccess(new AuditClientInfo(dispositivo, ip, hostname, userAgent, "", ""));
            }
        }
    }

    private String obtenerUserAgent() {
        String ua = System.getProperty("http.agent");
        return (ua != null && !ua.isEmpty()) ? ua : "Android/" + Build.VERSION.RELEASE + " InventarioPP";
    }

    /**
     * Obtiene la dirección IPv4 local del dispositivo.
     * Prioriza interfaces WiFi (wlan0) y de datos (rmnet), ignorando loopback y dummy.
     */
    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();

                // Ignorar interfaces desactivadas o loopback
                if (!intf.isUp() || intf.isLoopback()) continue;

                // Ignorar interfaces virtuales/dummy comunes
                String nombre = intf.getName().toLowerCase();
                if (nombre.contains("dummy") || nombre.contains("p2p")
                        || nombre.contains("tun") || nombre.contains("tap")) continue;

                for (Enumeration<InetAddress> addrs = intf.getInetAddresses();
                     addrs.hasMoreElements(); ) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "No se pudo obtener IP: " + e.getMessage());
        }
        return "";
    }
}