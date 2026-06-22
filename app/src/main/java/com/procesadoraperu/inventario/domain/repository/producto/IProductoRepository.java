package com.procesadoraperu.inventario.domain.repository.producto;

import com.procesadoraperu.inventario.domain.model.producto.Producto;

public interface IProductoRepository {

    // Consulta de stock en tiempo real al escanear un producto
    Producto fetchProductoStock(String idSucursal, String idAlmacen, String idProducto) throws Exception;
}