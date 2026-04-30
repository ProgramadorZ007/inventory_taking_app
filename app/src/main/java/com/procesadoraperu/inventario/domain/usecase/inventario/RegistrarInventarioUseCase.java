package com.procesadoraperu.inventario.domain.usecase.inventario;

import com.procesadoraperu.inventario.domain.model.inventario.Inventario;
import com.procesadoraperu.inventario.domain.model.log.LogIntegracion;
import com.procesadoraperu.inventario.domain.repository.inventario.IInventarioRepository;
import com.procesadoraperu.inventario.domain.repository.log.ILogRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RegistrarInventarioUseCase {

    private final IInventarioRepository inventarioRepository;
    private final ILogRepository logRepository; // NUEVO: Repositorio de Logs

    public RegistrarInventarioUseCase(IInventarioRepository inventarioRepository, ILogRepository logRepository) {
        this.inventarioRepository = inventarioRepository;
        this.logRepository = logRepository;
    }

    public void execute(Inventario inventario) {
        long startTime = System.currentTimeMillis(); // ⏱️ Inicia el cronómetro
        String fechaActual = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // Simulación básica del payload para el log
        String payload = "{idProducto: " + inventario.getIdProducto() + ", cantidad: " + inventario.getCantidad() + "}";

        // ¡LA CORRECCIÓN!: Definimos la referencia para saber qué registro generó este log
        String referencia = "Prod: " + inventario.getIdProducto();

        try {
            // 1. Intentamos enviar a la API
            inventarioRepository.enviarInventarioRemote(inventario);

            // Si llega aquí, fue un éxito.
            inventario.setEstadoSincronizacion("SINCRONIZADO");

            // ⏱️ Detiene cronómetro y guarda Log de ÉXITO
            long timeTaken = System.currentTimeMillis() - startTime;
            LogIntegracion logExito = new LogIntegracion(
                    "/api/almacen/inventarios", "POST", 200, payload, "OK", null,
                    timeTaken, fechaActual, inventario.getUsuarioCreacion(),
                    referencia // <-- Se inyecta aquí
            );
            logRepository.saveLogLocal(logExito);

        } catch (Exception e) {
            // 2. Falló el envío (No hay internet o servidor caído)
            inventario.setEstadoSincronizacion("PENDIENTE");
            // Usamos la fecha actual como fecha de registro local para ordenar los pendientes
            inventario.setFechaRegistroLocal(fechaActual);
            inventarioRepository.saveInventarioLocal(inventario);

            // ⏱️ Detiene cronómetro y guarda Log de ERROR
            long timeTaken = System.currentTimeMillis() - startTime;
            LogIntegracion logError = new LogIntegracion(
                    "/api/almacen/inventarios", "POST", 500, payload, null, e.getMessage(),
                    timeTaken, fechaActual, inventario.getUsuarioCreacion(),
                    referencia // <-- Se inyecta aquí
            );
            logRepository.saveLogLocal(logError);
        }
    }
}