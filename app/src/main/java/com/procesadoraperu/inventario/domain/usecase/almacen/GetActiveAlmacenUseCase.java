package com.procesadoraperu.inventario.domain.usecase.almacen;

import com.procesadoraperu.inventario.domain.model.almacen.Almacen;
import com.procesadoraperu.inventario.domain.repository.almacen.IAlmacenRepository;

public class GetActiveAlmacenUseCase {

    private final IAlmacenRepository almacenRepository;

    public GetActiveAlmacenUseCase(IAlmacenRepository almacenRepository) {
        this.almacenRepository = almacenRepository;
    }

    /**
     * Recupera el almacén en el que está trabajando actualmente el operario.
     */
    public Almacen execute() {
        return almacenRepository.getActiveAlmacen();
    }
}