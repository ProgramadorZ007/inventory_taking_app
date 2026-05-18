package com.procesadoraperu.inventario.domain.usecase.inventario;

import android.content.Context;

import com.procesadoraperu.inventario.core.sync.SyncScheduler;
import com.procesadoraperu.inventario.domain.model.inventario.Inventario;
import com.procesadoraperu.inventario.domain.model.log.LogIntegracion;
import com.procesadoraperu.inventario.domain.provider.IAuditClientInfoProvider;
import com.procesadoraperu.inventario.domain.repository.inventario.IInventarioRepository;
import com.procesadoraperu.inventario.domain.repository.log.ILogRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * MODIFICADO respecto al original:
 *
 * Cuando no hay conexión (catch), además de guardar localmente como PENDIENTE,
 * ahora programa un SyncScheduler.scheduleSyncWhenConnected() para que
 * WorkManager sincronice automáticamente cuando el dispositivo recupere red.
 *
 * El resto de la lógica es idéntica al original.
 */
public class RegistrarInventarioUseCase {

    private final IInventarioRepository inventarioRepository;
    private final ILogRepository logRepository;
    private final IAuditClientInfoProvider auditProvider;
    private final Context context; // NUEVO: necesario para SyncScheduler

    public RegistrarInventarioUseCase(IInventarioRepository inventarioRepository,
                                      ILogRepository logRepository,
                                      IAuditClientInfoProvider auditProvider,
                                      Context context) {  // NUEVO parámetro
        this.inventarioRepository = inventarioRepository;
        this.logRepository = logRepository;
        this.auditProvider = auditProvider;
        this.context = context.getApplicationContext(); // siempre usar appContext
    }

    public void execute(Inventario inventario) {
        auditProvider.getAuditInfo(auditInfo -> {
            inventario.setAuditClientInfo(auditInfo);
            ejecutarGuardadoYLog(inventario);
        });
    }

    private void ejecutarGuardadoYLog(Inventario inventario) {
        long startTime = System.currentTimeMillis();
        String fechaActual = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String payload = "{idProducto: " + inventario.getIdProducto() + ", cantidad: " + inventario.getCantidad() + "}";
        String referencia = "Prod: " + inventario.getIdProducto();

        try {
            // 1. Intentamos enviar a la API directamente
            inventarioRepository.enviarInventarioRemote(inventario);
            inventario.setEstadoSincronizacion("SINCRONIZADO");

            long timeTaken = System.currentTimeMillis() - startTime;
            logRepository.saveLogLocal(new LogIntegracion(
                    "/api/almacen/inventarios", "POST", 200, payload, "OK", null,
                    timeTaken, fechaActual, inventario.getUsuarioCreacion(), referencia
            ));

        } catch (Exception e) {
            // 2. Sin conexión → guardamos local como PENDIENTE
            inventario.setEstadoSincronizacion("PENDIENTE");
            inventario.setFechaRegistroLocal(fechaActual);
            inventarioRepository.saveInventarioLocal(inventario);

            long timeTaken = System.currentTimeMillis() - startTime;
            logRepository.saveLogLocal(new LogIntegracion(
                    "/api/almacen/inventarios", "POST", 500, payload, null, e.getMessage(),
                    timeTaken, fechaActual, inventario.getUsuarioCreacion(), referencia
            ));

            // ── NUEVO: Programar sync automático cuando haya conexión ──────────
            // WorkManager esperará silenciosamente hasta que el dispositivo
            // tenga red, luego ejecutará SyncInventarioWorker.
            SyncScheduler.scheduleSyncWhenConnected(context, inventario.getUsuarioCreacion());
        }
    }
}