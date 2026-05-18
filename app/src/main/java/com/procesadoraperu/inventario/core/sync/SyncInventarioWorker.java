package com.procesadoraperu.inventario.core.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.procesadoraperu.inventario.data.local.dao.InventarioDao;
import com.procesadoraperu.inventario.data.local.dao.LogDao;
import com.procesadoraperu.inventario.data.local.database.InventarioDatabase;
import com.procesadoraperu.inventario.data.remote.api.InventarioApi;
import com.procesadoraperu.inventario.data.repository.InventarioRepositoryImpl;
import com.procesadoraperu.inventario.data.repository.LogRepositoryImpl;
import com.procesadoraperu.inventario.domain.repository.inventario.IInventarioRepository;
import com.procesadoraperu.inventario.domain.repository.log.ILogRepository;
import com.procesadoraperu.inventario.domain.usecase.inventario.SincronizarPendientesUseCase;
import com.procesadoraperu.inventario.core.network.ApiClient;

public class SyncInventarioWorker extends Worker {

    private static final String TAG = "SyncInventarioWorker";
    public static final String KEY_USERNAME = "username";

    public SyncInventarioWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String username = getInputData().getString(KEY_USERNAME);

        if (username == null || username.isEmpty()) {
            Log.w(TAG, "No se encontró username en InputData. Abortando sync.");
            return Result.failure();
        }

        try {
            // Construir dependencias manualmente (sin DI framework)
            InventarioDatabase db = InventarioDatabase.getInstance(getApplicationContext());
            InventarioDao inventarioDao = db.inventarioDao();
            LogDao logDao = db.logDao();

            InventarioApi inventarioApi = ApiClient.getClient(getApplicationContext())
                    .create(InventarioApi.class);

            IInventarioRepository inventarioRepository =
                    new InventarioRepositoryImpl(inventarioApi, inventarioDao);
            ILogRepository logRepository =
                    new LogRepositoryImpl(logDao);

            SincronizarPendientesUseCase useCase =
                    new SincronizarPendientesUseCase(inventarioRepository, logRepository);

            int fallidos = useCase.execute(username);

            if (fallidos == 0) {
                Log.i(TAG, "Sincronización automática completada exitosamente para: " + username);
                return Result.success();
            } else {
                // Algunos fallaron: retry automático de WorkManager
                Log.w(TAG, fallidos + " registro(s) fallaron. WorkManager reintentará.");
                return Result.retry();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error inesperado en sincronización automática: " + e.getMessage(), e);
            return Result.retry();
        }
    }
}
