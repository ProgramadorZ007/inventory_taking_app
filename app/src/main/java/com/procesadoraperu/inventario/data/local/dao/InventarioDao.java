package com.procesadoraperu.inventario.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;
import com.procesadoraperu.inventario.data.local.entity.InventarioEntity;

@Dao
public interface InventarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(InventarioEntity inventario);

    @Query("SELECT * FROM Inventario WHERE usuarioCreacion = :username AND estadoSincronizacion = :estado ORDER BY fechaRegistroLocal DESC")
    List<InventarioEntity> getByStatusAndUser(String username, String estado);

    @Delete
    void delete(InventarioEntity inventario);

    @Query("DELETE FROM Inventario WHERE estadoSincronizacion = 'SINCRONIZADO'")
    void clearSynced();
}