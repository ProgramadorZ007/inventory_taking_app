package com.procesadoraperu.inventario.domain.usecase.producto;

import com.procesadoraperu.inventario.domain.model.producto.Producto;
import com.procesadoraperu.inventario.domain.repository.producto.IProductoRepository;

import java.util.List;

public class SincronizarCatalogoProductosUseCase {

    private final IProductoRepository productoRepository;

    public SincronizarCatalogoProductosUseCase(IProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    /**
     * Descarga la lista maestra de productos (detalles) y actualiza SQLite.
     * Elimina registros antiguos e inserta los nuevos para reflejar cambios de nombres.
     */
    public void execute() throws Exception {
        // 1. Descarga el catálogo completo desde la API detallada
        List<Producto> catalogoRemoto = productoRepository.fetchAllProductosRemote();

        if (catalogoRemoto != null && !catalogoRemoto.isEmpty()) {
            // 2. Sobrescribe la tabla 'Producto' en SQLite
            productoRepository.saveProductosLocal(catalogoRemoto);
        } else {
            throw new Exception("El servidor no devolvió productos para sincronizar.");
        }
    }
}