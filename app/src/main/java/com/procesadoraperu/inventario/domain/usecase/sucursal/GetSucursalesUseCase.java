package com.procesadoraperu.inventario.domain.usecase.sucursal;

import com.procesadoraperu.inventario.domain.model.sucursal.Sucursal;
import com.procesadoraperu.inventario.domain.repository.sucursal.ISucursalRepository;

import java.util.List;

public class GetSucursalesUseCase {

    private final ISucursalRepository sucursalRepository;

    public GetSucursalesUseCase(ISucursalRepository sucursalRepository) {
        this.sucursalRepository = sucursalRepository;
    }

    /**
     * Orquesta la obtención de sucursales priorizando la red si se fuerza la actualización,
     * o la caché local (SQLite) para trabajar Offline.
     * * @param forceRefresh Si es 'true', ignora SQLite y obliga a descargar de internet.
     */
    public List<Sucursal> execute(boolean forceRefresh) throws Exception {

        if (forceRefresh) {
            // El usuario o el sistema exigen datos frescos
            return fetchAndCacheRemote();
        }

        // 1. Intentamos obtener de la base de datos local (SQLite)
        List<Sucursal> sucursalesLocales = sucursalRepository.getSucursalesLocal();

        // 2. Si hay datos, los devolvemos inmediatamente (¡Rápido y Offline!)
        if (sucursalesLocales != null && !sucursalesLocales.isEmpty()) {
            return sucursalesLocales;
        }

        // 3. Si la tabla local está vacía (ej. primera vez que abre la app),
        // obligatoriamente vamos a internet.
        return fetchAndCacheRemote();
    }

    /**
     * Método auxiliar privado: Descarga de Nisira y guarda en SQLite
     */
    private List<Sucursal> fetchAndCacheRemote() throws Exception {
        // Consume Retrofit
        List<Sucursal> sucursalesRemotas = sucursalRepository.fetchSucursalesRemote();

        // Guarda en Room (SQLite)
        if (sucursalesRemotas != null && !sucursalesRemotas.isEmpty()) {
            sucursalRepository.saveSucursalesLocal(sucursalesRemotas);
        }

        return sucursalesRemotas;
    }

}
