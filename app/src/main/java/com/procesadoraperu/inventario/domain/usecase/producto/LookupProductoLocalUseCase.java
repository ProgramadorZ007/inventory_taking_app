package com.procesadoraperu.inventario.domain.usecase.producto;

import com.procesadoraperu.inventario.domain.model.producto.Producto;
import com.procesadoraperu.inventario.domain.repository.producto.IProductoRepository;

public class LookupProductoLocalUseCase {

    private final IProductoRepository productoRepository;

    public LookupProductoLocalUseCase(IProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    /**
     * Busca un producto en el catálogo local (offline) por su identificador.
     * No realiza ninguna llamada de red.
     * @param idProducto Código escaneado por el operario.
     * @return Objeto Producto encontrado en el catálogo local.
     * @throws IllegalArgumentException si el idProducto es nulo o vacío.
     * @throws Exception si el producto no se encuentra en el catálogo local.
     */
    public Producto execute(String idProducto) throws Exception {
        if (idProducto == null || idProducto.trim().isEmpty()) {
            throw new IllegalArgumentException("El código escaneado no contiene un identificador válido");
        }

        // Truncar a 50 caracteres como medida defensiva
        String idNormalizado = idProducto.trim();
        if (idNormalizado.length() > 50) {
            idNormalizado = idNormalizado.substring(0, 50);
        }

        // Búsqueda puramente local, sin acceso a red
        Producto producto = productoRepository.getProductoLocal(idNormalizado);

        if (producto == null) {
            throw new Exception("Producto no encontrado en el catálogo offline");
        }

        return producto;
    }
}
