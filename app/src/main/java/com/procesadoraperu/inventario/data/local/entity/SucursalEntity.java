package com.procesadoraperu.inventario.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Sucursal")
public class SucursalEntity {

    @PrimaryKey
    @NonNull
    public String idSucursal;

    public String descripcion;

    // Room necesita un constructor vacío o todos los campos públicos
    public SucursalEntity() {
        this.idSucursal = "";
    }

}
