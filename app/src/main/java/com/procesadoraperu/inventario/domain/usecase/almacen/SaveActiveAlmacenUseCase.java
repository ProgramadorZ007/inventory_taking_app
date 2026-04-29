package com.procesadoraperu.inventario.domain.usecase.almacen;

import com.procesadoraperu.inventario.domain.model.almacen.Almacen;
import com.procesadoraperu.inventario.domain.repository.almacen.IAlmacenRepository;

public class SaveActiveAlmacenUseCase {

    private final IAlmacenRepository almacenRepository;

    public SaveActiveAlmacenUseCase(IAlmacenRepository almacenRepository) {
        this.almacenRepository = almacenRepository;
    }

    /**
     * Guarda el Almacén seleccionado (ID y Nombre) en la sesión del equipo.
     */
    public void execute(Almacen almacenSeleccionado) {
        if (almacenSeleccionado == null || almacenSeleccionado.getIdAlmacen() == null) {
            throw new IllegalArgumentException("El almacén seleccionado no es válido");
        }

        almacenRepository.saveActiveAlmacen(almacenSeleccionado);
    }
}