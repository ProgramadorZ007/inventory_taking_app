package com.procesadoraperu.inventario.domain.usecase.usuario;

import com.procesadoraperu.inventario.domain.model.usuario.Usuario;
import com.procesadoraperu.inventario.domain.repository.usuario.IUsuarioRepository;

public class GetActiveUserUseCase {

    private final IUsuarioRepository usuarioRepository;

    public GetActiveUserUseCase(IUsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Devuelve el usuario autenticado actualmente.
     * Útil para la UI (mostrar nombres) y para auditoría (inyectar username en transacciones).
     * @throws Exception Si no se encuentra un usuario activo (la sesión está corrupta).
     */
    public Usuario execute() throws Exception {
        Usuario activeUser = usuarioRepository.getActiveUser();

        if (activeUser == null) {
            // Esto es una medida de seguridad. Si alguien intenta hacer un inventario
            // y SQLite no tiene al usuario, la app debe frenar el proceso.
            throw new Exception("No hay una sesión de usuario válida en el dispositivo.");
        }

        return activeUser;
    }
}