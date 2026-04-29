package com.procesadoraperu.inventario.domain.usecase.sucursal;

import com.procesadoraperu.inventario.domain.model.sucursal.Sucursal;

import java.util.ArrayList;
import java.util.List;

public class SearchSucursalUseCase {

    /**
     * Filtra una lista de sucursales en memoria basándose en un texto de búsqueda.
     * * @param query El texto que el operario está escribiendo.
     * @param sucursalesOriginales La lista completa obtenida por GetSucursalesUseCase.
     * @return Una nueva lista solo con las sucursales que coinciden.
     */
    public List<Sucursal> execute(String query, List<Sucursal> sucursalesOriginales) {

        // Si el usuario borró todo el texto o no hay nada escrito, devolvemos la lista completa
        if (query == null || query.trim().isEmpty()) {
            return sucursalesOriginales;
        }

        List<Sucursal> sucursalesFiltradas = new ArrayList<>();

        // Convertimos a minúsculas para que la búsqueda sea "Case Insensitive" (no importe mayúsculas)
        String textoBuscado = query.toLowerCase().trim();

        for (Sucursal sucursal : sucursalesOriginales) {
            // Evaluamos si la descripción de la sucursal contiene lo que el usuario escribió
            if (sucursal.getDescripcion() != null &&
                    sucursal.getDescripcion().toLowerCase().contains(textoBuscado)) {

                sucursalesFiltradas.add(sucursal);
            }
        }

        return sucursalesFiltradas;
    }
}