package com.procesadoraperu.inventario.domain.repository.log;

import com.procesadoraperu.inventario.domain.model.log.LogIntegracion;
import java.util.List;

public interface ILogRepository {

    /**
     * Guarda un registro de la petición HTTP en la base de datos local (SQLite).
     */
    void saveLogLocal(LogIntegracion log);

    /**
     * (Opcional) Obtiene los logs locales por si quieres mostrarlos en una
     * pantalla oculta de "Modo Desarrollador" en la app.
     */
    List<LogIntegracion> getLogsLocales();

    /**
     * (Opcional) Limpia los logs antiguos para que la base de datos no crezca infinitamente.
     */
    void clearOldLogs();
}