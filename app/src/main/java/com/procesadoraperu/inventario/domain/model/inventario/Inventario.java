package com.procesadoraperu.inventario.domain.model.inventario;

public class Inventario {

    // Identificadores Generales
    private int idInventario;
    private String idEmpresa;

    // Ubicación
    private String idSucursal;
    private String sucursal;
    private String idAlmacen;
    private String almacen;

    // Producto
    private String idProducto;
    private String producto;
    private String unidadMedida;

    // Cantidades
    private double stock;
    private double cantidad;

    // Auditoría
    private String usuarioCreacion;
    private String fechaCreacion;
    private String fechaRegistroLocal; // Fecha en que se realizó el reenvío
    private AuditClientInfo auditClientInfo;

    // Estado local para saber si subió a la nube o no
    // Valores posibles: "PENDIENTE", "SINCRONIZADO"
    private String estadoSincronizacion = "SINCRONIZADO";

    public String getEstadoSincronizacion() {
        return estadoSincronizacion;
    }

    public void setEstadoSincronizacion(String estadoSincronizacion) {
        this.estadoSincronizacion = estadoSincronizacion;
    }

    public Inventario() {
        // Constructor vacío para instanciar e ir llenando por partes
    }

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================

    public int getIdInventario() { return idInventario; }
    public void setIdInventario(int idInventario) { this.idInventario = idInventario; }

    public String getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(String idEmpresa) { this.idEmpresa = idEmpresa; }

    public String getIdSucursal() { return idSucursal; }
    public void setIdSucursal(String idSucursal) { this.idSucursal = idSucursal; }

    public String getSucursal() { return sucursal; }
    public void setSucursal(String sucursal) { this.sucursal = sucursal; }

    public String getIdAlmacen() { return idAlmacen; }
    public void setIdAlmacen(String idAlmacen) { this.idAlmacen = idAlmacen; }

    public String getAlmacen() { return almacen; }
    public void setAlmacen(String almacen) { this.almacen = almacen; }

    public String getIdProducto() { return idProducto; }
    public void setIdProducto(String idProducto) { this.idProducto = idProducto; }

    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }

    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }

    public double getStock() { return stock; }
    public void setStock(double stock) { this.stock = stock; }

    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }

    public String getUsuarioCreacion() { return usuarioCreacion; }
    public void setUsuarioCreacion(String usuarioCreacion) { this.usuarioCreacion = usuarioCreacion; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public AuditClientInfo getAuditClientInfo() { return auditClientInfo; }
    public void setAuditClientInfo(AuditClientInfo auditClientInfo) { this.auditClientInfo = auditClientInfo; }

    public String getFechaRegistroLocal() {
        return fechaRegistroLocal;
    }

    public void setFechaRegistroLocal(String fechaRegistroLocal) {
        this.fechaRegistroLocal = fechaRegistroLocal;
    }

}