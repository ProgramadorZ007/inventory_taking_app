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
    private final String username;
    private final String referenciaId; // NUEVO: Para saber qué registro provocó el log

    public String getEndpoint() {
        return endpoint;
    }

    public LogIntegracion(String endpoint, String metodoHttp, int codigoHttp,
                          String payloadEnvio, String respuestaErp, String detalleError,
                          long tiempoRespuestaMs, String fechaRegistro, String username,
                          String referenciaId) {
        this.endpoint = endpoint;
        this.metodoHttp = metodoHttp;
        this.codigoHttp = codigoHttp;
        this.payloadEnvio = payloadEnvio;
        this.respuestaErp = respuestaErp;
        this.detalleError = detalleError;
        this.tiempoRespuestaMs = tiempoRespuestaMs;
        this.fechaRegistro = fechaRegistro;
        this.username = username;
        this.referenciaId = referenciaId;
    }

    // ... (Mantienes tus getters actuales y agregas el nuevo)
    public String getReferenciaId() { return referenciaId; }

    public String getUsername() {
        return username;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public long getTiempoRespuestaMs() {
        return tiempoRespuestaMs;
    }

    public String getDetalleError() {
        return detalleError;
    }

    public String getRespuestaErp() {
        return respuestaErp;
    }

    public String getPayloadEnvio() {
        return payloadEnvio;
    }

    public int getCodigoHttp() {
        return codigoHttp;
    }

    public String getMetodoHttp() {
        return metodoHttp;
    }
}