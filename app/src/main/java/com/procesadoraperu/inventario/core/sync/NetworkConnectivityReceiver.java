package com.procesadoraperu.inventario.core.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * BroadcastReceiver que detecta cuando el dispositivo recupera conexión a internet
 * y dispara inmediatamente la sincronización de registros pendientes.
 *
 * IMPORTANTE: Declarado en AndroidManifest.xml con el filtro CONNECTIVITY_CHANGE.
 * Complementa a WorkManager: si la app está en primer plano, este receiver
 * reacciona instantáneamente sin esperar el scheduling de WorkManager.
 */
public class NetworkConnectivityReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkConnectivityRcvr";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            return;
        }

        if (isConnected(context)) {
            Log.d(TAG, "Red disponible. Encolando sincronización automática...");
            SyncScheduler.scheduleOnce(context);
        }
    }

    private boolean isConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
}
