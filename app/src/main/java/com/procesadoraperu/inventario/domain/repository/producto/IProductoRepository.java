package com.procesadoraperu.inventario.domain.repository.producto;

import com.procesadoraperu.inventario.domain.model.producto.Producto;
import java.util.List;

public interface IProductoRepository {

    // Consulta en tiempo real (Paso 1)
    Producto fetchProductoStock(String idSucursal, String idAlmacen, String idProducto) throws Exception;

    // Descarga masiva del catálogo (Paso 2)
    List<Producto> fetchAllProductosRemote() throws Exception;

    // Operaciones de base de datos local (SQLite)
    void saveProductosLocal(List<Producto> productos);
    Producto getProductoLocal(String idProducto);
}