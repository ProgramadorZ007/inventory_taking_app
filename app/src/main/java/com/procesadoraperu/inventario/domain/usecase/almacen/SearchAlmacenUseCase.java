package com.procesadoraperu.inventario.domain.usecase.almacen;

import com.procesadoraperu.inventario.domain.model.almacen.Almacen;

import java.util.ArrayList;
import java.util.List;

public class SearchAlmacenUseCase {

    public List<Almacen> execute(String query, List<Almacen> almacenesOriginales) {
        if (query == null || query.trim().isEmpty()) {
            return almacenesOriginales;
        }

        List<Almacen> almacenesFiltrados = new ArrayList<>();
        String textoBuscado = query.toLowerCase().trim();

        for (Almacen almacen : almacenesOriginales) {
            if (almacen.getDescripcion() != null &&
                    almacen.getDescripcion().toLowerCase().contains(textoBuscado)) {

                almacenesFiltrados.add(almacen);
            }
        }

        return almacenesFiltrados;
    }
}