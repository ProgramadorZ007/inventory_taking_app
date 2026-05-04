package com.procesadoraperu.inventario.domain.usecase.log;

import com.procesadoraperu.inventario.domain.model.log.LogIntegracion;
import com.procesadoraperu.inventario.domain.repository.log.ILogRepository;

import java.util.List;

public class GetLogsLocalesUseCase {

    private final ILogRepository logRepository;

    public GetLogsLocalesUseCase(ILogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public List<LogIntegracion> execute() {
        return logRepository.getLogsLocales();
    }
}