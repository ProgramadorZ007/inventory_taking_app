package com.procesadoraperu.inventario.domain.usecase.inventario;

import com.procesadoraperu.inventario.domain.model.inventario.Inventario;
import com.procesadoraperu.inventario.domain.repository.inventario.IInventarioRepository;
import java.util.List;

public class GetInventariosPendientesUseCase {

    private final IInventarioRepository inventarioRepository;

    public GetInventariosPendientesUseCase(IInventarioRepository inventarioRepository) {
        this.inventarioRepository = inventarioRepository;
    }

    public List<Inventario> execute(String username) {
        // Trae de SQLite solo los del operario actual que dicen "PENDIENTE"
        return inventarioRepository.getInventariosLocalesPorEstado(username, "PENDIENTE");
    }
}