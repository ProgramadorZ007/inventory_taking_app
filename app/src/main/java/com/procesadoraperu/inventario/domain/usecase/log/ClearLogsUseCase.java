package com.procesadoraperu.inventario.domain.usecase.log;

import com.procesadoraperu.inventario.domain.repository.log.ILogRepository;

public class ClearLogsUseCase {

    private final ILogRepository logRepository;

    public ClearLogsUseCase(ILogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public void execute() {
        logRepository.clearOldLogs();
    }
}