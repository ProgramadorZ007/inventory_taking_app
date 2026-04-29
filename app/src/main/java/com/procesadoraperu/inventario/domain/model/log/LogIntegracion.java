package com.procesadoraperu.inventario.domain.model.log;

public class LogIntegracion {

    private final String endpoint;
    private final String metodoHttp;
    private final int codigoHttp;
    private final String payloadEnvio;
    private final String respuestaErp;
    private final String detalleError;
    private final long tiempoRespuestaMs;
    private final String fechaRegistro;
    private final String username; // Quién provocó esta petición

    public LogIntegracion(String endpoint, String metodoHttp, int codigoHttp,
                          String payloadEnvio, String respuestaErp, String detalleError,
                          long tiempoRespuestaMs, String fechaRegistro, String username) {
        this.endpoint = endpoint;
        this.metodoHttp = metodoHttp;
        this.codigoHttp = codigoHttp;
        this.payloadEnvio = payloadEnvio;
        this.respuestaErp = respuestaErp;
        this.detalleError = detalleError;
        this.tiempoRespuestaMs = tiempoRespuestaMs;
        this.fechaRegistro = fechaRegistro;
        this.username = username;
    }

    // SOLO GETTERS
    public String getEndpoint() { return endpoint; }
    public String getMetodoHttp() { return metodoHttp; }
    public int getCodigoHttp() { return codigoHttp; }
    public String getPayloadEnvio() { return payloadEnvio; }
    public String getRespuestaErp() { return respuestaErp; }
    public String getDetalleError() { return detalleError; }
    public long getTiempoRespuestaMs() { return tiempoRespuestaMs; }
    public String getFechaRegistro() { return fechaRegistro; }
    public String getUsername() { return username; }
}