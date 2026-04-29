package com.procesadoraperu.inventario.domain.usecase.almacen;

import com.procesadoraperu.inventario.domain.model.almacen.Almacen;
import com.procesadoraperu.inventario.domain.repository.almacen.IAlmacenRepository;

import java.util.List;

public class GetAlmacenesUseCase {

    private final IAlmacenRepository almacenRepository;

    public GetAlmacenesUseCase(IAlmacenRepository almacenRepository) {
        this.almacenRepository = almacenRepository;
    }

    /**
     * Obtiene los almacenes de una sucursal específica.
     * Prioriza SQLite (Offline) a menos que se exija actualización.
     */
    public List<Almacen> execute(String idSucursal, boolean forceRefresh) throws Exception {
        if (idSucursal == null || idSucursal.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID de la sucursal es obligatorio para buscar almacenes");
        }

        if (forceRefresh) {
            return fetchAndCacheRemote(idSucursal);
        }

        // 1. Buscar en SQLite local filtrando por Sucursal
        List<Almacen> almacenesLocales = almacenRepository.getAlmacenesLocal(idSucursal);

        if (almacenesLocales != null && !almacenesLocales.isEmpty()) {
            return almacenesLocales;
        }

        // 2. Si no hay datos, ir a internet
        return fetchAndCacheRemote(idSucursal);
    }

    private List<Almacen> fetchAndCacheRemote(String idSucursal) throws Exception {
        List<Almacen> almacenesRemotos = almacenRepository.fetchAlmacenesRemote(idSucursal);

        if (almacenesRemotos != null && !almacenesRemotos.isEmpty()) {
            almacenRepository.saveAlmacenesLocal(almacenesRemotos);
        }

        return almacenesRemotos;
    }
}