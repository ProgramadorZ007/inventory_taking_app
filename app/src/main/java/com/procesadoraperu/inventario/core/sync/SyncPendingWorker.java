package com.procesadoraperu.inventario.core.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.procesadoraperu.inventario.core.network.ApiClient;
import com.procesadoraperu.inventario.data.local.database.InventarioDatabase;
import com.procesadoraperu.inventario.data.remote.api.InventarioApi;
import com.procesadoraperu.inventario.data.repository.InventarioRepositoryImpl;
import com.procesadoraperu.inventario.data.repository.LogRepositoryImpl;
import com.procesadoraperu.inventario.data.repository.UsuarioRepositoryImpl;
import com.procesadoraperu.inventario.domain.model.usuario.Usuario;
import com.procesadoraperu.inventario.domain.repository.inventario.IInventarioRepository;
import com.procesadoraperu.inventario.domain.repository.log.ILogRepository;
import com.procesadoraperu.inventario.domain.usecase.inventario.SincronizarPendientesUseCase;
import com.procesadoraperu.inventario.domain.usecase.usuario.GetActiveUserUseCase;

import retrofit2.Retrofit;

/**
 * Worker de WorkManager que ejecuta la sincronización automática de registros
 * PENDIENTES cuando el dispositivo recupera conexión a internet.
 *
 * La constraint NETWORK_CONNECTED garantiza que solo se ejecuta con red disponible.
 * Si falla por error transitorio, WorkManager reintentará automáticamente.
 */
public class SyncPendingWorker extends Worker {

    private static final String TAG = "SyncPendingWorker";

    public SyncPendingWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Iniciando sincronización automática de pendientes...");

        try {
            Context ctx = getApplicationContext();

            // ── Construir dependencias (igual que ViewModelFactory) ──────────────
            InventarioDatabase db = InventarioDatabase.getInstance(ctx);
            Retrofit retrofit     = ApiClient.getClient(ctx);

            IInventarioRepository invRepo = new InventarioRepositoryImpl(
                    retrofit.create(InventarioApi.class),
                    db.inventarioDao()
            );
            ILogRepository logRepo = new LogRepositoryImpl(db.logDao());

            GetActiveUserUseCase getActiveUserUseCase =
                    new GetActiveUserUseCase(new UsuarioRepositoryImpl(db.usuarioDao()));

            SincronizarPendientesUseCase sincronizarUseCase =
                    new SincronizarPendientesUseCase(invRepo, logRepo);

            // ── Verificar que hay sesión activa ──────────────────────────────────
            Usuario usuario;
            try {
                usuario = getActiveUserUseCase.execute();
            } catch (Exception e) {
                // Sin sesión activa: no es un error, simplemente no aplica
                Log.d(TAG, "Sin usuario activo, se omite la sincronización automática.");
                return Result.success();
            }

            // ── Ejecutar sincronización ──────────────────────────────────────────
            int fallidos = sincronizarUseCase.execute(usuario.getUsername());

            if (fallidos == 0) {
                Log.d(TAG, "Sincronización automática completada sin errores.");
            } else {
                Log.w(TAG, "Sincronización completada con " + fallidos + " fallo(s). Se reintentará.");
                // Devolvemos retry para que WorkManager vuelva a intentarlo
                return Result.retry();
            }

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Error crítico en SyncPendingWorker: " + e.getMessage(), e);
            return Result.retry();
        }
    }
}
