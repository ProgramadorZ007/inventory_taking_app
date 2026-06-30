package com.procesadoraperu.inventario.core.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/**
 * Utilidad para verificar el estado de conexión a internet.
 * Usa las APIs modernas de NetworkCapabilities (API 23+).
 */
public class InternetUtil {

    private InternetUtil() {}

    /**
     * Verifica si el dispositivo tiene una conexión a internet activa y validada.
     * @param context Contexto de la aplicación
     * @return true si hay internet disponible, false en caso contrario
     */
    public static boolean hayInternet(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities =
                connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null) {
            return false;
        }

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
