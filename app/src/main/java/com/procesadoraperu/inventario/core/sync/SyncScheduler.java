package com.procesadoraperu.inventario.core.sync;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Utilidad para programar y cancelar el Worker de sincronización.
 *
 * USO:
 *   // Al guardar un inventario PENDIENTE:
 *   SyncScheduler.scheduleSyncWhenConnected(context, username);
 *
 *   // Al cerrar sesión:
 *   SyncScheduler.cancelSync(context);
 *
 * WorkManager se encarga automáticamente de:
 *  - Esperar a que haya conexión (Constraints.CONNECTED)
 *  - Reintentar con backoff exponencial si falla
 *  - Persistir la tarea aunque el dispositivo se reinicie
 */
public class SyncScheduler {

    // Nombre único para evitar Workers duplicados
    private static final String WORK_NAME = "sync_inventarios_pendientes";

    /**
     * Registra una tarea de sincronización que se ejecutará tan pronto
     * como el dispositivo tenga conexión a internet.
     *
     * Si ya existe una tarea pendiente con el mismo nombre, la MANTIENE
     * (KEEP) para no cancelar la que ya estaba esperando conexión.
     *
     * @param context  contexto de la app (Application o Activity)
     * @param username nombre del operario activo (para filtrar sus pendientes)
     */
    public static void scheduleSyncWhenConnected(Context context, String username) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Data inputData = new Data.Builder()
                .putString(SyncInventarioWorker.KEY_USERNAME, username)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncInventarioWorker.class)
                .setConstraints(constraints)
                .setInputData(inputData)
                // Backoff exponencial: reintenta a los 30s, 1m, 2m, 4m...
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(
                        WORK_NAME,
                        ExistingWorkPolicy.KEEP,   // no cancelar si ya hay uno esperando
                        syncRequest
                );
    }

    /**
     * Cancela cualquier tarea de sincronización pendiente.
     * Llamar al cerrar sesión.
     */
    public static void cancelSync(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }
}