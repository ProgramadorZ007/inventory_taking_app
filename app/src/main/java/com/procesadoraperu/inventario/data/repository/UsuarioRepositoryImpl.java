package com.procesadoraperu.inventario.data.repository;

import com.procesadoraperu.inventario.data.local.dao.UsuarioDao;
import com.procesadoraperu.inventario.data.local.entity.UsuarioEntity;
import com.procesadoraperu.inventario.domain.model.usuario.Usuario;
import com.procesadoraperu.inventario.domain.repository.usuario.IUsuarioRepository;

public class UsuarioRepositoryImpl implements IUsuarioRepository {

    private final UsuarioDao usuarioDao;

    public UsuarioRepositoryImpl(UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    @Override
    public Usuario getActiveUser() {
        UsuarioEntity entity = usuarioDao.getUsuarioActivo();
        if (entity == null) return null;
        return new Usuario(entity.username, entity.nombres, entity.idCodigoGeneral);
    }
}