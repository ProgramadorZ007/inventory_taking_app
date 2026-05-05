package com.procesadoraperu.inventario.data.local.entity;

public class AuditClientInfoEntity {
    public String dispositivo;
    public String ip;
    public String hostname;
    public String userAgent;
    public String latitud;
    public String longitud;

    public AuditClientInfoEntity() {}

    public AuditClientInfoEntity(String dispositivo, String ip, String hostname,
                                 String userAgent, String latitud, String longitud) {
        this.dispositivo = dispositivo;
        this.ip = ip;
        this.hostname = hostname;
        this.userAgent = userAgent;
        this.latitud = latitud;
        this.longitud = longitud;
    }
}