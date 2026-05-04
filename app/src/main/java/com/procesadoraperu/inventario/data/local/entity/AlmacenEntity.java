package com.procesadoraperu.inventario.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Almacen")
public class AlmacenEntity {

    @PrimaryKey
    @NonNull
    public String idAlmacen;

    public String idSucursal;
    public String descripcion;

    public AlmacenEntity() {
        this.idAlmacen = "";
    }
}