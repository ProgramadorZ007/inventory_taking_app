package com.procesadoraperu.inventario.data.remote.request;

import com.google.gson.annotations.SerializedName;

public class RegistrarInventarioRequest {

    @SerializedName("idEmpresa")
    public String idEmpresa;

    @SerializedName("idSucursal")
    public String idSucursal;

    @SerializedName("idAlmacen")
    public String idAlmacen;

    @SerializedName("idProducto")
    public String idProducto;

    @SerializedName("dscProducto")
    public String dscProducto;

    @SerializedName("idMedida")
    public String idMedida;

    @SerializedName("stock")
    public double stock;

    @SerializedName("cantidad")
    public double cantidad;

    @SerializedName("auditClientInfo")
    public AuditInfo auditClientInfo;

    public RegistrarInventarioRequest() {}

    // Clase interna para la auditoría (se serializa como objeto JSON anidado)
    public static class AuditInfo {
        @SerializedName("dispositivo")
        public String dispositivo;

        @SerializedName("ip")
        public String ip;

        @SerializedName("hostname")
        public String hostname;

        @SerializedName("userAgent")
        public String userAgent;

        @SerializedName("latitud")
        public String latitud;

        @SerializedName("longitud")
        public String longitud;
    }
}