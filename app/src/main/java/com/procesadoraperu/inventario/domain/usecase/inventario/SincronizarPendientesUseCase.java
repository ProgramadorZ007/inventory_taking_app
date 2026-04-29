package com.procesadoraperu.inventario.domain.usecase.inventario;

import com.procesadoraperu.inventario.domain.model.inventario.Inventario;
import com.procesadoraperu.inventario.domain.model.log.LogIntegracion;
import com.procesadoraperu.inventario.domain.repository.inventario.IInventarioRepository;
import com.procesadoraperu.inventario.domain.repository.log.ILogRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SincronizarPendientesUseCase {

    private final IInventarioRepository inventarioRepository;
    private final ILogRepository logRepository; // NUEVO

    public SincronizarPendientesUseCase(IInventarioRepository inventarioRepository, ILogRepository logRepository) {
        this.inventarioRepository = inventarioRepository;
        this.logRepository = logRepository;
    }

    public int execute(String username) {
        List<Inventario> pendientes = inventarioRepository.getInventariosLocalesPorEstado(username, "PENDIENTE");
        int fallidos = 0;

        for (Inventario inv : pendientes) {
            long startTime = System.currentTimeMillis();
            String fechaActual = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String payload = "{idProducto: " + inv.getIdProducto() + ", cantidad: " + inv.getCantidad() + "}";

            try {
                // Intentamos enviarlo de nuevo
                inventarioRepository.enviarInventarioRemote(inv);

                // Si tuvo éxito, lo borramos de la BD local
                inventarioRepository.deleteInventarioLocal(inv);

                // Guardamos Log de Éxito en el reintento
                long timeTaken = System.currentTimeMillis() - startTime;
                LogIntegracion logExito = new LogIntegracion(
                        "/api/almacen/inventarios (Reintento)", "POST", 200, payload, "OK", null,
                        timeTaken, fechaActual, username
                );
                logRepository.saveLogLocal(logExito);

            } catch (Exception e) {
                fallidos++;

                // Guardamos Log de Error en el reintento
                long timeTaken = System.currentTimeMillis() - startTime;
                LogIntegracion logError = new LogIntegracion(
                        "/api/almacen/inventarios (Reintento)", "POST", 500, payload, null, e.getMessage(),
                        timeTaken, fechaActual, username
                );
                logRepository.saveLogLocal(logError);
            }
        }
        return fallidos;
    }
}