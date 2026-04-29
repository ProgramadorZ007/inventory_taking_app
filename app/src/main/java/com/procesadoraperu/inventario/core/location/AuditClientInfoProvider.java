package com.procesadoraperu.inventario.core.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.procesadoraperu.inventario.domain.model.inventario.AuditClientInfo;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class AuditClientInfoProvider {

    private final FusedLocationProviderClient fusedLocationClient;

    public interface OnAuditInfoCallback {
        void onSuccess(AuditClientInfo auditInfo);
    }

    public AuditClientInfoProvider(Context context) {
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public void getAuditInfo(OnAuditInfoCallback callback) {
        // Obtenemos los datos inmutables primero
        final String dispositivo = Build.MANUFACTURER + " " + Build.MODEL;
        final String hostname = Build.DEVICE;
        final String ip = getLocalIpAddress();

        // Manejo del UserAgent para que sea "final"
        String uaTemp = System.getProperty("http.agent");
        final String userAgent = (uaTemp != null) ? uaTemp : "Android / Procesadora Peru App";

        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                String lat = "";
                String lon = "";

                if (location != null) {
                    lat = String.valueOf(location.getLatitude());
                    lon = String.valueOf(location.getLongitude());
                }

                // Creamos el objeto usando las variables finales
                callback.onSuccess(new AuditClientInfo(
                        dispositivo, ip, hostname, userAgent, lat, lon
                ));
            }).addOnFailureListener(e -> {
                // Si falla el GPS, enviamos vacío en coordenadas
                callback.onSuccess(new AuditClientInfo(
                        dispositivo, ip, hostname, userAgent, "", ""
                ));
            });
        } catch (Exception e) {
            callback.onSuccess(new AuditClientInfo(
                    dispositivo, ip, hostname, userAgent, "", ""
            ));
        }
    }

    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }
}