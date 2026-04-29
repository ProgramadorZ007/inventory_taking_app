package com.procesadoraperu.inventario.domain.model.sucursal;

public class Sucursal {

    /*
    Atributos estructurales inmutables
    ==================================
    */
    private final String idSucursal;
    private final String descripcion;

    /*
    Constructor
    ===========
    */
    public Sucursal(String idSucursal, String descripcion) {
        this.idSucursal = idSucursal;
        this.descripcion = descripcion;
    }

    /*
    SOLO GETTERS (Sin Setters para proteger la integridad)
    ======================================================
    */
    public String getIdSucursal() {
        return idSucursal;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /*
    Sobrescritura útil para las vistas (Spinners de Android)
    ========================================================
    */
    @Override
    public String toString() {
        return descripcion;
    }
}