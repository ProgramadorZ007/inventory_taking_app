package com.procesadoraperu.inventario.data.remote.request;

import com.google.gson.annotations.SerializedName;

public class ProductoStockRequest {

    @SerializedName("idSucursal")
    private String idSucursal;

    @SerializedName("idAlmacen")
    private String idAlmacen;

    @SerializedName("idProducto")
    private String idProducto;

    public ProductoStockRequest(String idSucursal, String idAlmacen, String idProducto) {
        this.idSucursal = idSucursal;
        this.idAlmacen = idAlmacen;
        this.idProducto = idProducto;
    }
}