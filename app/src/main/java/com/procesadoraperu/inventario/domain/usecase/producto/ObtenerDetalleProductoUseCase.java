package com.procesadoraperu.inventario.domain.usecase.producto;

import com.procesadoraperu.inventario.domain.model.producto.Producto;
import com.procesadoraperu.inventario.domain.repository.producto.IProductoRepository;

public class ObtenerDetalleProductoUseCase {

    private final IProductoRepository productoRepository;

    public ObtenerDetalleProductoUseCase(IProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    /**
     * Recupera todos los detalles (cultivo, variedad, estado) de un producto.
     * Ideal para mostrar el detalle al hacer clic en un registro de inventario ya tomado.
     * @param idProducto Código del producto a buscar.
     */
    public Producto execute(String idProducto) throws Exception {
        // Busca en la base de datos local (Room/SQLite) para que sea instantáneo
        Producto productoDetallado = productoRepository.getProductoLocal(idProducto);

        if (productoDetallado == null) {
            throw new Exception("Detalles del producto no encontrados en la base de datos local. Por favor, sincronice el catálogo.");
        }

        return productoDetallado;
    }
}