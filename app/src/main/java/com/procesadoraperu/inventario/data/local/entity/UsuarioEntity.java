package com.procesadoraperu.inventario.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Usuario")
public class UsuarioEntity {

    @PrimaryKey
    @NonNull
    public String username;

    public String nombres;
    public String idCodigoGeneral;

    public UsuarioEntity() {
        this.username = "";
    }

}
