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

    //La clase no interactúa directamente con bases de datos. En su lugar, recibe interfaces.
    private final IInventarioRepository inventarioRepository;
    private final ILogRepository logRepository;

    // 1. Inyección de repositorios (desacoplamiento)
    public SincronizarPendientesUseCase(IInventarioRepository inventarioRepository, ILogRepository logRepository) {
        this.inventarioRepository = inventarioRepository;
        this.logRepository = logRepository;
    }

    // 2. Búsqueda con restricción operativa por usuario y estado
    public int execute(String username) {
        //Cuando se ejecuta el método execute, consulta la base de datos local para buscar todos los registros de inventario de ese usuario específico
        //que no han podido ser enviados al servidor central y tienen el estado "PENDIENTE".
        List<Inventario> pendientes = inventarioRepository.getInventariosLocalesPorEstado(username, "PENDIENTE");
        int fallidos = 0;

        for (Inventario inv : pendientes) {
            long startTime = System.currentTimeMillis();
            //Para calcular cuántos milisegundos tarda la petición (útil para auditorías de rendimiento).
            String fechaActual = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String payload = "{idProducto: " + inv.getIdProducto() + ", cantidad: " + inv.getCantidad() + "}";
            //Construye un JSON básico en texto con el ID del producto y la cantidad.

            String referencia = "LocalID: " + inv.getIdInventario();
            //Guarda el ID local del inventario.

            try {
                // Intentamos enviarlo de nuevo
                inventarioRepository.enviarInventarioRemote(inv);

                // Si tuvo éxito, lo borramos de la BD local
                inventarioRepository.deleteInventarioLocal(inv);

                // Guardamos Log de Éxito en el reintento
                long timeTaken = System.currentTimeMillis() - startTime;
                LogIntegracion logExito = new LogIntegracion(
                        "/api/almacen/inventarios (Reintento)", "POST", 200, payload, "OK", null,
                        timeTaken, fechaActual, username,
                        referencia
                );
                logRepository.saveLogLocal(logExito);

            } catch (Exception e) {
                // Tolerancia a fallos: suma un error pero el sistema no crashea
                //Si el servidor remoto está caído, da un error 500, o el dispositivo volvió a perder internet en medio del proceso, la excepción es capturada.
                fallidos++;

                // Guardamos Log de Error en el reintento
                long timeTaken = System.currentTimeMillis() - startTime;
                LogIntegracion logError = new LogIntegracion(
                        "/api/almacen/inventarios (Reintento)", "POST", 500, payload, null, e.getMessage(),
                        timeTaken, fechaActual, username,
                        referencia
                );
                logRepository.saveLogLocal(logError);
            }
        }
        return fallidos;
    }
}