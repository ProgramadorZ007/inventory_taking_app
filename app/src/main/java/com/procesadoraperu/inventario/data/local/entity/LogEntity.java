package com.procesadoraperu.inventario.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "logs_integracion")
public class LogEntity {

    @PrimaryKey(autoGenerate = true)
    public int idLog;

    public String endpoint;
    public String metodoHttp;
    public int codigoHttp;
    public String payloadEnvio;
    public String respuestaErp;
    public String detalleError;
    public long tiempoRespuestaMs;
    public String fechaRegistro;
    public String username;
    public String referenciaId;

    public LogEntity() {}
}