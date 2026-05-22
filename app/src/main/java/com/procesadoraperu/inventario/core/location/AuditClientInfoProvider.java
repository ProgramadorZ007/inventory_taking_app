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
 * Estrategia GPS (3 intentos en cascada, optimizada para interiores):
 *   1. getLastLocation()                       — instantáneo, usa caché del proveedor
 *   2. getCurrentLocation(BALANCED_POWER)      — red/WiFi, funciona en interiores, timeout 6 seg
 *   3. requestLocationUpdates(BALANCED_POWER)  — fuerza escaneo de red, timeout 10 seg
 *
 * Se usa PRIORITY_BALANCED_POWER_ACCURACY (WiFi + red móvil) porque la app
 * opera principalmente en almacenes cerrados donde el GPS satelital no penetra.
 * Si los 3 fallan, se registra con lat/lon vacíos (nunca se bloquea el flujo).
 */
public class AuditClientInfoProvider implements IAuditClientInfoProvider {

    private static final String TAG = "AuditClientInfoProvider";

    // Timeouts ajustados: la ubicación por red es mucho más rápida que el GPS
    private static final long TIMEOUT_GET_CURRENT_MS   = 6_000L;
    private static final long TIMEOUT_REQUEST_UPDATE_MS = 10_000L;

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

        // ── Intento 1: getLastLocation (instantáneo, usa caché) ──────────────
        // Es la fuente más rápida. Si el dispositivo usó ubicación recientemente
        // (Maps, otra app, etc.), devuelve coordenadas en milisegundos.
        intentoGetLastLocation(dispositivo, ip, hostname, userAgent, callback);
    }

    /**
     * Intento 1: getLastLocation — última coordenada en caché del proveedor.
     * Rápido (sin espera), pero puede ser null si el GPS no se usó recientemente.
     */
    @SuppressLint("MissingPermission")
    private void intentoGetLastLocation(String dispositivo, String ip, String hostname,
                                        String userAgent, OnAuditInfoCallback callback) {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            Log.d(TAG, "✅ LastLocation OK: "
                                    + location.getLatitude() + ", " + location.getLongitude());
                            callback.onSuccess(new AuditClientInfo(
                                    dispositivo, ip, hostname, userAgent,
                                    String.valueOf(location.getLatitude()),
                                    String.valueOf(location.getLongitude())
                            ));
                        } else {
                            // Caché vacía → intentar ubicación por red (funciona en interiores)
                            Log.w(TAG, "LastLocation null → intentando getCurrentLocation (BALANCED)");
                            intentoGetCurrentLocation(dispositivo, ip, hostname, userAgent, callback);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "getLastLocation falló: " + e.getMessage());
                        intentoGetCurrentLocation(dispositivo, ip, hostname, userAgent, callback);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Excepción en getLastLocation: " + e.getMessage());
            intentoGetCurrentLocation(dispositivo, ip, hostname, userAgent, callback);
        }
    }

    /**
     * Intento 2: getCurrentLocation con PRIORITY_BALANCED_POWER_ACCURACY.
     * Usa WiFi y red móvil — funciona en interiores (almacenes, oficinas).
     * Timeout de TIMEOUT_GET_CURRENT_MS ms para no bloquear el flujo.
     */
    @SuppressLint("MissingPermission")
    private void intentoGetCurrentLocation(String dispositivo, String ip, String hostname,
                                           String userAgent, OnAuditInfoCallback callback) {

        final AtomicBoolean respondido = new AtomicBoolean(false);
        final Handler handler = new Handler(Looper.getMainLooper());
        final CancellationTokenSource cts = new CancellationTokenSource();

        // Timeout: si en TIMEOUT_GET_CURRENT_MS no llegó respuesta, pasamos al siguiente
        Runnable timeoutRunnable = () -> {
            if (respondido.compareAndSet(false, true)) {
                Log.w(TAG, "getCurrentLocation timeout → intentando requestLocationUpdates");
                cts.cancel();
                intentoRequestUpdates(dispositivo, ip, hostname, userAgent, callback);
            }
        };
        handler.postDelayed(timeoutRunnable, TIMEOUT_GET_CURRENT_MS);

        try {
            // CORRECCIÓN CLAVE: BALANCED_POWER usa WiFi + red móvil,
            // HIGH_ACCURACY solo funciona bien al aire libre con GPS satelital.
            fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.getToken())
                    .addOnSuccessListener(location -> {
                        handler.removeCallbacks(timeoutRunnable);
                        if (respondido.compareAndSet(false, true)) {
                            if (location != null) {
                                Log.d(TAG, "✅ getCurrentLocation (BALANCED) OK: "
                                        + location.getLatitude() + ", " + location.getLongitude());
                                callback.onSuccess(new AuditClientInfo(
                                        dispositivo, ip, hostname, userAgent,
                                        String.valueOf(location.getLatitude()),
                                        String.valueOf(location.getLongitude())
                                ));
                            } else {
                                Log.w(TAG, "getCurrentLocation devolvió null → requestLocationUpdates");
                                intentoRequestUpdates(dispositivo, ip, hostname, userAgent, callback);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        handler.removeCallbacks(timeoutRunnable);
                        if (respondido.compareAndSet(false, true)) {
                            Log.w(TAG, "getCurrentLocation falló: " + e.getMessage());
                            intentoRequestUpdates(dispositivo, ip, hostname, userAgent, callback);
                        }
                    });

        } catch (Exception e) {
            handler.removeCallbacks(timeoutRunnable);
            if (respondido.compareAndSet(false, true)) {
                Log.e(TAG, "Excepción en getCurrentLocation: " + e.getMessage());
                intentoRequestUpdates(dispositivo, ip, hostname, userAgent, callback);
            }
        }
    }

    /**
     * Intento 3: requestLocationUpdates con BALANCED_POWER.
     * Fuerza al proveedor a escanear WiFi/redes disponibles.
     * Timeout final de TIMEOUT_REQUEST_UPDATE_MS ms.
     */
    @SuppressLint("MissingPermission")
    private void intentoRequestUpdates(String dispositivo, String ip, String hostname,
                                       String userAgent, OnAuditInfoCallback callback) {

        final AtomicBoolean respondido = new AtomicBoolean(false);
        final Handler handler = new Handler(Looper.getMainLooper());

        // CORRECCIÓN: BALANCED_POWER para funcionar en interiores
        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY, 1000)
                .setMaxUpdates(1)
                .setWaitForAccurateLocation(false)
                .build();

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                handler.removeCallbacksAndMessages(null);
                if (respondido.compareAndSet(false, true)) {
                    fusedLocationClient.removeLocationUpdates(this);
                    if (locationResult != null && locationResult.getLastLocation() != null) {
                        android.location.Location loc = locationResult.getLastLocation();
                        Log.d(TAG, "✅ RequestUpdates (BALANCED) OK: "
                                + loc.getLatitude() + ", " + loc.getLongitude());
                        callback.onSuccess(new AuditClientInfo(
                                dispositivo, ip, hostname, userAgent,
                                String.valueOf(loc.getLatitude()),
                                String.valueOf(loc.getLongitude())
                        ));
                    } else {
                        Log.w(TAG, "RequestUpdates sin resultado → registrando sin coordenadas");
                        callback.onSuccess(new AuditClientInfo(
                                dispositivo, ip, hostname, userAgent, "", ""));
                    }
                }
            }
        };

        // Timeout final: rendirse y registrar sin coordenadas
        handler.postDelayed(() -> {
            if (respondido.compareAndSet(false, true)) {
                try { fusedLocationClient.removeLocationUpdates(locationCallback); }
                catch (Exception ignored) {}
                Log.w(TAG, "RequestUpdates timeout → registrando sin coordenadas");
                callback.onSuccess(new AuditClientInfo(
                        dispositivo, ip, hostname, userAgent, "", ""));
            }
        }, TIMEOUT_REQUEST_UPDATE_MS);

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
                callback.onSuccess(new AuditClientInfo(
                        dispositivo, ip, hostname, userAgent, "", ""));
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

                if (!intf.isUp() || intf.isLoopback()) continue;

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