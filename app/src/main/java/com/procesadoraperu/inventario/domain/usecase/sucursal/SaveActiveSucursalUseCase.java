package com.procesadoraperu.inventario.domain.usecase.sucursal;

import com.procesadoraperu.inventario.domain.repository.sucursal.ISucursalRepository;

public class SaveActiveSucursalUseCase {

    private final ISucursalRepository sucursalRepository;

    public SaveActiveSucursalUseCase(ISucursalRepository sucursalRepository) {
        this.sucursalRepository = sucursalRepository;
    }

    /**
     * Guarda el ID de la sucursal seleccionada para usarlo en la pantalla de Almacenes.
     * * @param idSucursal El ID de la sucursal que el operario seleccionó.
     */
    public void execute(String idSucursal) throws IllegalArgumentException {
        if (idSucursal == null || idSucursal.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID de la sucursal no puede estar vacío");
        }

        sucursalRepository.saveActiveSucursalId(idSucursal);
    }
}