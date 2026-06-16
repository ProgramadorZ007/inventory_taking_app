package com.procesadoraperu.inventario.data.repository;

import com.procesadoraperu.inventario.data.local.dao.LogDao;
import com.procesadoraperu.inventario.data.local.entity.LogEntity;
import com.procesadoraperu.inventario.domain.model.log.LogIntegracion;
import com.procesadoraperu.inventario.domain.repository.log.ILogRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * IMPLEMENTACIÓN DEL REPOSITORIO DE LOGS DE INTEGRACIÓN
 * 
 * Esta clase es responsable de gestionar la persistencia local de la auditoría 
 * de red. Captura cada interacción con los servicios web de NISIRA/PPSAC para 
 * garantizar la trazabilidad y fiabilidad del sistema bajo estándares ISO/IEC 25010.
 *
 * Autor: Procesadora Perú S.A.C.
 */
public class LogRepositoryImpl implements ILogRepository {

    private final LogDao logDao;

    /**
     * Constructor que inyecta el Objeto de Acceso a Datos (DAO)
     * @param logDao Interfaz Room para operaciones en SQLite
     */
    public LogRepositoryImpl(LogDao logDao) {
        this.logDao = logDao;
    }

    /**
     * Guarda un registro de auditoría de integración en la base de datos local.
     * @param log Objeto del dominio con la información de la petición HTTP y respuesta del ERP.
     */
    @Override
    public void saveLogLocal(LogIntegracion log) {
        // Mapea el objeto del dominio a una entidad de base de datos antes de insertar
        logDao.insert(mapToEntity(log));
    }

    /**
     * Recupera el historial completo de logs almacenados en el dispositivo.
     * @return Lista de logs convertidos al modelo de dominio.
     */
    @Override
    public List<LogIntegracion> getLogsLocales() {
        return mapListToDomain(logDao.getAllLogs());
    }

    /**
     * Elimina todos los registros de auditoría locales para liberar espacio.
     */
    @Override
    public void clearOldLogs() {
        logDao.clearAllLogs();
    }

    // ==========================================
    // MAPPERS (CONVERSORES DE DATOS)
    // ==========================================

    /**
     * Convierte un modelo de Negocio (Domain) a un modelo de Base de Datos (Entity).
     * Esto mantiene la capa de datos aislada de la lógica de negocio.
     */
    private LogEntity mapToEntity(LogIntegracion d) {
        LogEntity e = new LogEntity();
        e.endpoint = d.getEndpoint();
        e.metodoHttp = d.getMetodoHttp();
        e.codigoHttp = d.getCodigoHttp();
        e.payloadEnvio = d.getPayloadEnvio();
        e.respuestaErp = d.getRespuestaErp();
        e.detalleError = d.getDetalleError();
        e.tiempoRespuestaMs = d.getTiempoRespuestaMs();
        e.fechaRegistro = d.getFechaRegistro();
        e.username = d.getUsername();
        e.referenciaId = d.getReferenciaId();
        return e;
    }

    /**
     * Convierte una lista de entidades de base de datos a objetos de dominio.
     */
    private List<LogIntegracion> mapListToDomain(List<LogEntity> entities) {
        List<LogIntegracion> list = new ArrayList<>();
        if (entities != null) {
            for (LogEntity e : entities) {
                list.add(new LogIntegracion(
                        e.endpoint, e.metodoHttp, e.codigoHttp,
                        e.payloadEnvio, e.respuestaErp, e.detalleError,
                        e.tiempoRespuestaMs, e.fechaRegistro, e.username, e.referenciaId
                ));
            }
        }
        return list;
    }
}