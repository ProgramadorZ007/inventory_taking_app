package com.procesadoraperu.inventario.domain.repository.producto;

import com.procesadoraperu.inventario.domain.model.producto.Producto;

public interface IProductoRepository {

    // Consulta de stock en tiempo real al escanear un producto
    Producto fetchProductoStock(String idSucursal, String idAlmacen, String idProducto) throws Exception;

    // Descargar catálogo completo y almacenar localmente
    int downloadAndStoreCatalog(String idSucursal, String idAlmacen) throws Exception;

    // Búsqueda local de producto por ID
    Producto getProductoLocal(String idProducto);

    // Limpiar catálogo local
    void clearLocalCatalog();

    // Obtener cantidad de productos almacenados localmente
    int getLocalProductCount();
}