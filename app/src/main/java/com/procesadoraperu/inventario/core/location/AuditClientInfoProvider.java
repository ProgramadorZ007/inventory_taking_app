package com.procesadoraperu.inventario.core.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.procesadoraperu.inventario.domain.model.inventario.AuditClientInfo;
import com.procesadoraperu.inventario.domain.provider.IAuditClientInfoProvider;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Proveedor de información de auditoría del cliente.
 *
 * CORRECCIÓN GPS: Se reemplazó getLastLocation() (que devuelve null si no hay
 * ubicación reciente en caché) por getCurrentLocation() con PRIORITY_HIGH_ACCURACY,
 * que solicita activamente una ubicación fresca al hardware GPS/Red.
 * Si getCurrentLocation() también falla, cae en getLastLocation() como fallback.
 */
public class AuditClientInfoProvider implements IAuditClientInfoProvider {

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

        // ── Intento 1: getCurrentLocation (ubicación FRESCA del GPS) ────────
        // A diferencia de getLastLocation(), este método enciende el GPS si es necesario.
        CancellationTokenSource cts = new CancellationTokenSource();

        try {
            fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            // ✅ GPS devolvió coordenadas frescas
                            String lat = String.valueOf(location.getLatitude());
                            String lon = String.valueOf(location.getLongitude());
                            callback.onSuccess(new AuditClientInfo(
                                    dispositivo, ip, hostname, userAgent, lat, lon));
                        } else {
                            // getCurrentLocation devolvió null → fallback a última conocida
                            intentarUltimaUbicacion(dispositivo, ip, hostname, userAgent, callback);
                        }
                    })
                    .addOnFailureListener(e ->
                            // getCurrentLocation falló → fallback a última conocida
                            intentarUltimaUbicacion(dispositivo, ip, hostname, userAgent, callback));

        } catch (Exception e) {
            // El servicio de ubicación no está disponible → retornar sin coords
            callback.onSuccess(new AuditClientInfo(dispositivo, ip, hostname, userAgent, "", ""));
        }
    }

    /**
     * Intento 2 (fallback): getLastLocation — retorna la última coordenada en caché.
     * Puede ser null si el GPS no ha sido usado recientemente o está desactivado.
     */
    @SuppressLint("MissingPermission")
    private void intentarUltimaUbicacion(String dispositivo, String ip, String hostname,
                                         String userAgent, OnAuditInfoCallback callback) {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        String lat = (location != null) ? String.valueOf(location.getLatitude()) : "";
                        String lon = (location != null) ? String.valueOf(location.getLongitude()) : "";
                        callback.onSuccess(new AuditClientInfo(
                                dispositivo, ip, hostname, userAgent, lat, lon));
                    })
                    .addOnFailureListener(e ->
                            callback.onSuccess(new AuditClientInfo(
                                    dispositivo, ip, hostname, userAgent, "", "")));
        } catch (Exception e) {
            callback.onSuccess(new AuditClientInfo(dispositivo, ip, hostname, userAgent, "", ""));
        }
    }

    private String obtenerUserAgent() {
        String ua = System.getProperty("http.agent");
        return (ua != null && !ua.isEmpty()) ? ua : "Android/" + Build.VERSION.RELEASE + " InventarioPP";
    }

    /**
     * Obtiene la dirección IPv4 local del dispositivo.
     * Ignora loopback y direcciones IPv6.
     */
    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> addrs = intf.getInetAddresses();
                     addrs.hasMoreElements(); ) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) { }
        return "127.0.0.1";
    }
}