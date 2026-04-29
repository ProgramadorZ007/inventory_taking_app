package com.procesadoraperu.inventario.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.procesadoraperu.inventario.data.local.entity.UsuarioEntity;

@Dao
public interface UsuarioDao {

    // Obtiene el perfil del usuario activo (solo debería haber uno)
    @Query("SELECT * FROM Usuario LIMIT 1")
    UsuarioEntity getUsuarioActivo();

    // Guarda el perfil del usuario tras un login exitoso
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UsuarioEntity usuario);

    // Limpia la tabla cuando el usuario hace Logout
    @Query("DELETE FROM Usuario")
    void deleteUsuario();

}
