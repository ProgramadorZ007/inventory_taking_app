package com.procesadoraperu.inventario.core.sync;

import android.content.Context;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Utilidad centralizada para programar la sincronización automática con WorkManager.
 *
 * USO:
 *   - Al registrar un inventario PENDIENTE → SyncScheduler.scheduleOnce(context)
 *   - Al detectar que volvió la red       → SyncScheduler.scheduleOnce(context)
 *   - Al hacer login                       → SyncScheduler.scheduleOnce(context)
 *
 * WorkManager garantiza que:
 *   1. Solo se ejecuta cuando hay red disponible (constraint CONNECTED).
 *   2. Si hay un trabajo ya encolado con la misma tag, no se duplica (KEEP policy).
 *   3. Si la app se cierra o el dispositivo reinicia, el trabajo persiste.
 */
public class SyncScheduler {

    private static final String TAG        = "SyncScheduler";
    public  static final String WORK_NAME  = "sync_pending_inventarios";

    private SyncScheduler() {}

    /**
     * Encola UN trabajo de sincronización que se ejecutará en cuanto haya red.
     * Si ya hay uno encolado con el mismo nombre, lo respeta (no duplica).
     */
    public static void scheduleOnce(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncPendingWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30, TimeUnit.SECONDS   // 30s → 60s → 120s … (máx 5h por WorkManager)
                )
                .addTag(WORK_NAME)
                .build();

        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(
                        WORK_NAME,
                        ExistingWorkPolicy.KEEP,   // No reemplazar si ya hay uno esperando
                        request
                );

        Log.d(TAG, "Trabajo de sincronización encolado (se ejecutará cuando haya red).");
    }

    /**
     * Cancela cualquier trabajo de sincronización pendiente.
     * Útil al hacer logout para no sincronizar con otra sesión.
     */
    public static void cancel(Context context) {
        WorkManager.getInstance(context.getApplicationContext())
                .cancelUniqueWork(WORK_NAME);
        Log.d(TAG, "Trabajo de sincronización cancelado.");
    }
}