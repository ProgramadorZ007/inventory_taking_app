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

        List<Inventario> historialCompleto = inventarioRepository.fetchHistorialRemote(
                idSucursal, idAlmacen, fechaInicio, fechaFin
        );

        List<Inventario> historialDelUsuario = new ArrayList<>();

        if (historialCompleto != null && username != null) {
            // CORRECCIÓN: Normalizar ambos lados a minúsculas para comparación robusta.
            // El servidor puede devolver "OPERARIO01" y localmente guardarmos "operario01".
            String usernameLower = username.trim().toLowerCase();

            for (Inventario inv : historialCompleto) {
                if (inv.getUsuarioCreacion() != null &&
                        inv.getUsuarioCreacion().trim().toLowerCase().equals(usernameLower)) {
                    historialDelUsuario.add(inv);
                }
            }
        }

        return historialDelUsuario;
    }
}