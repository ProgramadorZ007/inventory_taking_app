package com.procesadoraperu.inventario.core.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.procesadoraperu.inventario.domain.model.inventario.AuditClientInfo;
import com.procesadoraperu.inventario.domain.provider.IAuditClientInfoProvider;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Proveedor de información de auditoría del cliente.
 *
 * Esta clase se encarga de recolectar metadatos del dispositivo (IP, Modelo, User Agent)
 * y coordenadas geográficas (Latitud/Longitud) para garantizar la trazabilidad
 * de las operaciones de inventario realizadas en campo.
 *
 * Implementa {@link IAuditClientInfoProvider} siguiendo el principio de inversión
 * de dependencias de Clean Architecture.
 */
public class AuditClientInfoProvider implements IAuditClientInfoProvider {

    private final FusedLocationProviderClient fusedLocationClient;

    /**
     * Constructor del proveedor de auditoría.
     *
     * @param context Contexto de la aplicación necesario para inicializar
     *                el cliente de servicios de ubicación de Google.
     */
    public AuditClientInfoProvider(Context context) {
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    /**
     * Obtiene de forma asíncrona la información de auditoría completa.
     *
     * Recopila datos de hardware, red y ubicación. En caso de que el GPS esté
     * desactivado o falle, el proceso continúa retornando los datos técnicos
     * con las coordenadas vacías para no bloquear la operación principal.
     *
     * @param callback Interfaz de respuesta para retornar el objeto {@link AuditClientInfo}.
     */
    @Override
    @SuppressLint("MissingPermission")
    public void getAuditInfo(OnAuditInfoCallback callback) {

        // 1. Recolección de datos de identidad del hardware
        final String dispositivo = Build.MANUFACTURER + " " + Build.MODEL;
        final String hostname = Build.DEVICE;
        final String ip = getLocalIpAddress();

        // 2. Obtención del User Agent del sistema
        String uaTemp = System.getProperty("http.agent");
        final String userAgent = (uaTemp != null) ? uaTemp : "Android / Procesadora Peru App";

        try {
            // 3. Intento de obtención de la última ubicación conocida (Last Location)
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        String lat = "";
                        String lon = "";

                        if (location != null) {
                            lat = String.valueOf(location.getLatitude());
                            lon = String.valueOf(location.getLongitude());
                        }

                        // Retorno exitoso con datos de ubicación (si existen)
                        callback.onSuccess(new AuditClientInfo(
                                dispositivo, ip, hostname, userAgent, lat, lon
                        ));
                    })
                    .addOnFailureListener(e -> {
                        // Retorno preventivo: Se envían datos técnicos aunque falle el GPS
                        callback.onSuccess(new AuditClientInfo(
                                dispositivo, ip, hostname, userAgent, "", ""
                        ));
                    });

        } catch (Exception e) {
            // Manejo de excepciones críticas para asegurar que el flujo nunca se detenga
            callback.onSuccess(new AuditClientInfo(
                    dispositivo, ip, hostname, userAgent, "", ""
            ));
        }
    }

    /**
     * Obtiene la dirección IP local (IPv4) asignada al dispositivo en la red actual.
     *
     * Recorre las interfaces de red activas (Wi-Fi o Datos) buscando una dirección
     * que no sea de tipo Loopback (127.0.0.1).
     *
     * @return String con la dirección IP local o "127.0.0.1" si no se detecta red.
     */
    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();

                    // Filtramos para obtener solo IPv4 y omitir la dirección interna
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
            // El error se ignora para evitar interrupciones; se retorna el default.
        }
        return "127.0.0.1";
    }
}