package com.procesadoraperu.inventario.domain.model.producto;

import java.math.BigDecimal;

public class Producto {

    /*
    1. ATRIBUTOS DEL PRIMER PASO (Inmutables)
    Se obtienen al escanear con /api/nisira/producto-stock
    ======================================================
    */
    private final String idEmpresa;     // NUEVO: Para usarlo al guardar el inventario
    private final String idProducto;
    private final String descripcion;
    private final String idMedida;
    private final String idGrupo;
    private final String grupo;         // Ajustado según nueva API
    private final String idSubGrupo;    // Ajustado según nueva API
    private final String subGrupo;      // Ajustado según nueva API
    private final String ultFecha;      // NUEVO: Última fecha de movimiento

    /*
    2. ATRIBUTOS DE STOCK (Dinámicos)
    =================================
    */
    private BigDecimal stock;
    private BigDecimal disponible;      // NUEVO: Stock real disponible para usar

    /*
    3. ATRIBUTOS DEL SEGUNDO PASO (Mutables / Lazy Loading)
    Se obtienen al consultar detalles adicionales si es necesario
    =======================================================
    */
    private String idCultivo;
    private String cultivo;
    private String idVariedad;
    private String variedad;
    private Integer estado;

    /*
    Constructor para el PRIMER PASO (Escaneo inicial)
    Actualizado con el nuevo JSON de Nisira
    =====================================================
    */
    public Producto(String idEmpresa, String idProducto, String descripcion, String idMedida,
                    String idGrupo, String grupo, String idSubGrupo, String subGrupo,
                    BigDecimal stock, BigDecimal disponible, String ultFecha) {

        this.idEmpresa = idEmpresa;
        this.idProducto = idProducto;
        this.descripcion = descripcion;
        this.idMedida = idMedida;
        this.idGrupo = idGrupo;
        this.grupo = grupo;
        this.idSubGrupo = idSubGrupo;
        this.subGrupo = subGrupo;
        this.stock = stock != null ? stock : BigDecimal.ZERO;
        this.disponible = disponible != null ? disponible : BigDecimal.ZERO;
        this.ultFecha = ultFecha;

        // Inicializados en null hasta enriquecerlos
        this.idCultivo = null;
        this.cultivo = null;
        this.idVariedad = null;
        this.variedad = null;
        this.estado = null;
    }

    /*
    MÉTODOS DE NEGOCIO
    ==================
    */
    public void enriquecerDatosAgro(String idCultivo, String cultivo, String idVariedad, String variedad, Integer estado) {
        this.idCultivo = idCultivo;
        this.cultivo = cultivo;
        this.idVariedad = idVariedad;
        this.variedad = variedad;
        this.estado = estado;
    }

    public void actualizarStock(BigDecimal nuevoStock, BigDecimal nuevoDisponible) {
        if (nuevoStock != null && nuevoStock.compareTo(BigDecimal.ZERO) >= 0) {
            this.stock = nuevoStock;
        }
        if (nuevoDisponible != null && nuevoDisponible.compareTo(BigDecimal.ZERO) >= 0) {
            this.disponible = nuevoDisponible;
        }
    }

    public boolean isActivo() {
        return this.estado != null && this.estado == 1;
    }

    /*
    GETTERS (Incluyendo los nuevos)
    ===============================
    */
    public String getIdEmpresa() { return idEmpresa; }
    public String getIdProducto() { return idProducto; }
    public String getDescripcion() { return descripcion; }
    public String getIdMedida() { return idMedida; }
    public String getIdGrupo() { return idGrupo; }
    public String getGrupo() { return grupo; }
    public String getIdSubGrupo() { return idSubGrupo; }
    public String getSubGrupo() { return subGrupo; }
    public BigDecimal getStock() { return stock; }
    public BigDecimal getDisponible() { return disponible; }
    public String getUltFecha() { return ultFecha; }

    public String getIdCultivo() { return idCultivo; }
    public String getCultivo() { return cultivo; }
    public String getIdVariedad() { return idVariedad; }
    public String getVariedad() { return variedad; }
    public Integer getEstado() { return estado; }
}