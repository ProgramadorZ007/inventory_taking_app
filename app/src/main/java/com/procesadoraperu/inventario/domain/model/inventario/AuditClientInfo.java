package com.procesadoraperu.inventario.domain.model.inventario;

public class AuditClientInfo {

    private final String dispositivo;
    private final String ip;
    private final String hostname;
    private final String userAgent;
    private final String latitud;
    private final String longitud;

    public AuditClientInfo(String dispositivo, String ip, String hostname, String userAgent, String latitud, String longitud) {
        this.dispositivo = dispositivo;
        this.ip = ip;
        this.hostname = hostname;
        this.userAgent = userAgent;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    // SOLO GETTERS
    public String getDispositivo() { return dispositivo; }
    public String getIp() { return ip; }
    public String getHostname() { return hostname; }
    public String getUserAgent() { return userAgent; }
    public String getLatitud() { return latitud; }
    public String getLongitud() { return longitud; }
}