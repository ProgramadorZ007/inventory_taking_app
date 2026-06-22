package com.procesadoraperu.inventario.domain.usecase.producto;

import com.procesadoraperu.inventario.domain.repository.producto.IProductoRepository;

public class DownloadCatalogUseCase {

    private final IProductoRepository productoRepository;

    public DownloadCatalogUseCase(IProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    /**
     * Descarga el catálogo completo de productos y lo almacena localmente.
     * @param idSucursal ID de la sucursal activa.
     * @param idAlmacen ID del almacén activo.
     * @return Cantidad de productos descargados y almacenados.
     */
    public int execute(String idSucursal, String idAlmacen) throws Exception {
        if (idSucursal == null || idSucursal.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID de la sucursal no puede estar vacío");
        }
        if (idAlmacen == null || idAlmacen.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del almacén no puede estar vacío");
        }

        return productoRepository.downloadAndStoreCatalog(idSucursal, idAlmacen);
    }
}
