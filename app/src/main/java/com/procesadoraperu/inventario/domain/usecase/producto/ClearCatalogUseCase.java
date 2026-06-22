package com.procesadoraperu.inventario.domain.usecase.producto;

import com.procesadoraperu.inventario.domain.repository.producto.IProductoRepository;

public class ClearCatalogUseCase {

    private final IProductoRepository productoRepository;

    public ClearCatalogUseCase(IProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public void execute() {
        productoRepository.clearLocalCatalog();
    }
}
