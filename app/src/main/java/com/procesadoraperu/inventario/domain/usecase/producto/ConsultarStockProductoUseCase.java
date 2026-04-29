package com.procesadoraperu.inventario.domain.usecase.producto;

import com.procesadoraperu.inventario.domain.model.producto.Producto;
import com.procesadoraperu.inventario.domain.repository.producto.IProductoRepository;

public class ConsultarStockProductoUseCase {

    private final IProductoRepository productoRepository;

    public ConsultarStockProductoUseCase(IProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    /**
     * Consulta el stock en tiempo real al momento de escanear el QR.
     * @param idSucursal ID de la sucursal activa.
     * @param idAlmacen ID del almacén activo.
     * @param idProducto Código escaneado por el operario.
     * @return Objeto Producto con datos básicos y stock actualizado.
     */
    public Producto execute(String idSucursal, String idAlmacen, String idProducto) throws Exception {
        if (idProducto == null || idProducto.trim().isEmpty()) {
            throw new IllegalArgumentException("El código del producto no puede estar vacío");
        }

        // Llamada directa al servidor para obtener el stock real
        Producto productoBasico = productoRepository.fetchProductoStock(idSucursal, idAlmacen, idProducto);

        if (productoBasico == null) {
            throw new Exception("El producto no existe o no tiene stock en este almacén.");
        }

        return productoBasico;
    }
}