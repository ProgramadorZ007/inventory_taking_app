package com.procesadoraperu.inventario.data.repository;

import com.procesadoraperu.inventario.data.local.dao.LogDao;
import com.procesadoraperu.inventario.data.local.entity.LogEntity;
import com.procesadoraperu.inventario.domain.model.log.LogIntegracion;
import com.procesadoraperu.inventario.domain.repository.log.ILogRepository;

import java.util.ArrayList;
import java.util.List;

public class LogRepositoryImpl implements ILogRepository {

    private final LogDao logDao;

    public LogRepositoryImpl(LogDao logDao) {
        this.logDao = logDao;
    }

    @Override
    public void saveLogLocal(LogIntegracion log) {
        logDao.insert(mapToEntity(log));
    }

    @Override
    public List<LogIntegracion> getLogsLocales() {
        return mapListToDomain(logDao.getAllLogs());
    }

    @Override
    public void clearOldLogs() {
        logDao.clearAllLogs();
    }

    // ==========================================
    // MAPPERS
    // ==========================================

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