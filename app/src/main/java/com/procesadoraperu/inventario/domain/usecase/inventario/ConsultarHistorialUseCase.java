package com.procesadoraperu.inventario.domain.usecase.inventario;

import com.procesadoraperu.inventario.domain.model.inventario.Inventario;
import com.procesadoraperu.inventario.domain.repository.inventario.IInventarioRepository;
import java.util.ArrayList;
import java.util.List;

public class ConsultarHistorialUseCase {

    private final IInventarioRepository inventarioRepository;

    public ConsultarHistorialUseCase(IInventarioRepository inventarioRepository) {
        this.inventarioRepository = inventarioRepository;
    }

    public List<Inventario> execute(String idSucursal, String idAlmacen,
                                    String fechaInicio, String fechaFin, String username) throws Exception {

        // 1. Traemos la data del servidor
        List<Inventario> historialCompleto = inventarioRepository.fetchHistorialRemote(
                idSucursal, idAlmacen, fechaInicio, fechaFin
        );

        // 2. Filtramos para mostrarle al operario SOLO los que él hizo
        List<Inventario> historialDelUsuario = new ArrayList<>();

        if (historialCompleto != null) {
            for (Inventario inv : historialCompleto) {
                if (inv.getUsuarioCreacion() != null &&
                        inv.getUsuarioCreacion().equalsIgnoreCase(username)) {
                    historialDelUsuario.add(inv);
                }
            }
        }

        return historialDelUsuario;
    }
}