package com.procesadoraperu.inventario.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Inventario")
public class InventarioEntity {

    @PrimaryKey(autoGenerate = true)
    public int idInventario;

    public String idEmpresa;
    public String idSucursal;
    public String sucursal;
    public String idAlmacen;
    public String almacen;
    public String idProducto;
    public String producto;
    public String unidadMedida;
    public double stock;
    public double cantidad;
    public String usuarioCreacion;
    public String fechaCreacion;
    public String fechaRegistroLocal;
    public String estadoSincronizacion;

    @Embedded(prefix = "audit_")
    public AuditClientInfoEntity auditClientInfo;

    public InventarioEntity() {}
}

// Clase de apoyo para el embebido
class AuditClientInfoEntity {
    public String dispositivo;
    public String ip;
    public String hostname;
    public String userAgent;
    public String latitud;
    public String longitud;
}