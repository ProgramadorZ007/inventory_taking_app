package com.procesadoraperu.inventario.domain.model.almacen;

public class Almacen {

    /*
    Atributos estructurales inmutables
    ==================================
    */
    private final String idAlmacen;
    private final String idSucursal; // Llave foránea lógica
    private final String descripcion;

    /*
    Constructor
    ===========
    */
    public Almacen(String idAlmacen, String idSucursal, String descripcion) {
        this.idAlmacen = idAlmacen;
        this.idSucursal = idSucursal;
        this.descripcion = descripcion;
    }

    /*
    SOLO GETTERS
    ============
    */
    public String getIdAlmacen() {
        return idAlmacen;
    }

    public String getIdSucursal() {
        return idSucursal;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}