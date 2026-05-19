package com.procesadoraperu.inventario.data.local.entity;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Inventario")
public class InventarioEntity {

    @PrimaryKey
    @androidx.annotation.NonNull
    public String idInventario = "";

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
    public String estadoSincronizacion; // "PENDIENTE" | "SINCRONIZADO"

    @Embedded(prefix = "audit_")
    public AuditClientInfoEntity auditClientInfo;

    public InventarioEntity() {}
}