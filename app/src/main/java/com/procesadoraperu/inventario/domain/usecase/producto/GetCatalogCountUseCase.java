package com.procesadoraperu.inventario.domain.usecase.producto;

import com.procesadoraperu.inventario.domain.repository.producto.IProductoRepository;

public class GetCatalogCountUseCase {

    private final IProductoRepository productoRepository;

    public GetCatalogCountUseCase(IProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    /**
     * Obtiene la cantidad de productos almacenados localmente en el catálogo.
     * @return Cantidad de productos en el catálogo local.
     */
    public int execute() {
        return productoRepository.getLocalProductCount();
    }
}
