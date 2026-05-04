package com.procesadoraperu.inventario.domain.provider;

import com.procesadoraperu.inventario.domain.model.inventario.AuditClientInfo;

public interface IAuditClientInfoProvider {

    // Movemos el callback aquí para que el Dominio sea el dueño de la regla
    interface OnAuditInfoCallback {
        void onSuccess(AuditClientInfo auditInfo);
    }

    void getAuditInfo(OnAuditInfoCallback callback);
}