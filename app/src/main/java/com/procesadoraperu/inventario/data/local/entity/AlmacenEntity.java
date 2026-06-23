package com.procesadoraperu.inventario.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "Almacen", primaryKeys = {"idAlmacen", "idSucursal"})
public class AlmacenEntity {

    @NonNull
    public String idAlmacen;

    @NonNull
    public String idSucursal;

    public String descripcion;

    public AlmacenEntity() {
        this.idAlmacen = "";
        this.idSucursal = "";
    }
}