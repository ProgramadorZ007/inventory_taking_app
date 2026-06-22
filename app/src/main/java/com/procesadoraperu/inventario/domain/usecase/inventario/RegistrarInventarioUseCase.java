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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegistrarInventarioUseCase {

    public interface OnRegistroCallback {
        void onSincronizado();
        void onGuardadoLocal();
        void onError(Exception e);
    }

    private final IInventarioRepository inventarioRepository;
    private final ILogRepository logRepository;
    private final IAuditClientInfoProvider auditProvider;
    private final Context context;

    // Hilo de fondo exclusivo para este UseCase
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public RegistrarInventarioUseCase(IInventarioRepository inventarioRepository,
                                      ILogRepository logRepository,
                                      IAuditClientInfoProvider auditProvider,
                                      Context context) {
        this.inventarioRepository = inventarioRepository;
        this.logRepository = logRepository;
        this.auditProvider = auditProvider;
        this.context = context.getApplicationContext();
    }

    public void execute(Inventario inventario, OnRegistroCallback callback) {
        // Pedimos la info del dispositivo (GPS, IP). Esto es asíncrono y su callback
        // puede volver en el MainThread (hilo de la interfaz gráfica).
        auditProvider.getAuditInfo(auditInfo -> {
            inventario.setAuditClientInfo(auditInfo);

            // CORRECCIÓN CRÍTICA:
            // Forzamos explícitamente que todo el proceso de guardado y red se ejecute
            // dentro del ExecutorService (hilo de fondo), escapando del MainThread.
            executor.execute(() -> ejecutarGuardadoYLog(inventario, callback));
        });
    }

    // Este método ya NO tiene un executor adentro, asume que quien lo llama
    // ya lo puso en un hilo secundario (como hicimos justo arriba).
    private void ejecutarGuardadoYLog(Inventario inventario, OnRegistroCallback callback) {
        try {
            long startTime = System.currentTimeMillis();
            String fechaActual = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            String payload = "{idProducto: " + inventario.getIdProducto() + ", cantidad: " + inventario.getCantidad() + "}";
            String referencia = "Prod: " + inventario.getIdProducto();

            try {
                // 1. Envío a API (Llamada de red - requiere estar fuera del MainThread)
                inventarioRepository.enviarInventarioRemote(inventario);

                // Éxito:
                inventario.setEstadoSincronizacion("SINCRONIZADO");

                long timeTaken = System.currentTimeMillis() - startTime;
                LogIntegracion logExito = new LogIntegracion(
                        "/api/almacen/inventarios", "POST", 200, payload, "OK", null,
                        timeTaken, fechaActual, inventario.getUsuarioCreacion(),
                        referencia
                );
                // (Acceso a Room - requiere estar fuera del MainThread)
                logRepository.saveLogLocal(logExito);

                callback.onSincronizado();

            } catch (Exception e) {
                // 2. Falló el envío (ej. sin internet), guardamos local
                inventario.setEstadoSincronizacion("PENDIENTE");
                inventario.setFechaRegistroLocal(fechaActual);

                // (Acceso a Room - requiere estar fuera del MainThread)
                inventarioRepository.saveInventarioLocal(inventario);

                // Programar sincronización automática cuando vuelva la red
                try {
                    SyncScheduler.scheduleOnce(context);
                } catch (Exception ignored) {
                    // No bloquear el flujo si WorkManager no está disponible
                }

                long timeTaken = System.currentTimeMillis() - startTime;
                LogIntegracion logError = new LogIntegracion(
                        "/api/almacen/inventarios", "POST", 500, payload, null, e.getMessage(),
                        timeTaken, fechaActual, inventario.getUsuarioCreacion(),
                        referencia
                );
                // (Acceso a Room - requiere estar fuera del MainThread)
                logRepository.saveLogLocal(logError);

                callback.onGuardadoLocal();
            }
        } catch (Exception e) {
            // 3. Error inesperado fuera del flujo normal (ej. error al construir el log)
            callback.onError(e);
        }
    }
}