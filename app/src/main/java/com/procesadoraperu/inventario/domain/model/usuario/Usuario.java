package com.procesadoraperu.inventario.domain.model.usuario;

public class Usuario {

    private final String username;
    private final String nombres;
    private final String idCodigoGeneral;

    public Usuario(String username, String nombres, String idCodigoGeneral) {
        this.username = username;
        this.nombres = nombres;
        this.idCodigoGeneral = idCodigoGeneral;
    }

    public String getUsername() { return username; }
    public String getNombres() { return nombres; }
    public String getIdCodigoGeneral() { return idCodigoGeneral; }

}
