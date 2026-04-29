package com.procesadoraperu.inventario.domain.usecase.sucursal;

import com.procesadoraperu.inventario.domain.repository.sucursal.ISucursalRepository;

public class GetActiveSucursalUseCase {

    private final ISucursalRepository sucursalRepository;

    public GetActiveSucursalUseCase(ISucursalRepository sucursalRepository) {
        this.sucursalRepository = sucursalRepository;
    }

    /**
     * Recupera el ID de la sucursal en la que el operario está trabajando actualmente.
     */
    public String execute() {
        return sucursalRepository.getActiveSucursalId();
    }
}