package com.procesadoraperu.inventario.domain.repository.usuario;

import com.procesadoraperu.inventario.domain.model.usuario.Usuario;

public interface IUsuarioRepository {

    /**
     * Recupera el usuario que actualmente tiene la sesión iniciada en el dispositivo.
     * Lee directamente de la base de datos local (SQLite/Room).
     * @return El objeto Usuario, o null si por algún error no hay usuario guardado.
     */
    Usuario getActiveUser();

}