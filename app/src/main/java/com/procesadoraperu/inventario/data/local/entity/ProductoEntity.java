package com.procesadoraperu.inventario.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Producto")
public class ProductoEntity {

    @PrimaryKey
    @NonNull
    public String idProducto;

    public String idEmpresa;
    public String descripcion;
    public String idMedida;
    public String idGrupo;
    public String grupoDsc; // Coincide con JSON
    public String idSubGrupo;
    public String subgrupoDsc; // Coincide con JSON
    public String ultFecha;

    // Usamos Double para SQLite, en el Domain se convertirá a BigDecimal
    public Double stock;
    public Double disponible;

    // Campos del Catálogo (Paso 2)
    public String nombreComercial;
    public String idUbicacion;
    public String tipoproducto;
    public String propiedad;
    public String idCultivo;
    public String cultivo;
    public String idVariedad;
    public String variedad;
    public Integer estado;

    public ProductoEntity() {
        this.idProducto = "";
    }
}