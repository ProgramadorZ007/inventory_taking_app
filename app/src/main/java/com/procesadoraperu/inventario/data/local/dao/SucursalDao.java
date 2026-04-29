package com.procesadoraperu.inventario.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import com.procesadoraperu.inventario.data.local.entity.SucursalEntity;
import java.util.List;

@Dao
public interface SucursalDao {

    // Devuelve todas las sucursales
    @Query("SELECT * FROM Sucursal")
    List<SucursalEntity> getAll();

    // Inserta una lista completa. Si ya existe el ID, lo reemplaza (actualiza)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SucursalEntity> sucursales);

    // Borra todas las sucursales (útil si hay una actualización masiva o cierre de sesión)
    @Query("DELETE FROM Sucursal")
    void deleteAll();

    // Transacción limpia: Borra lo viejo y mete lo nuevo de golpe
    @Transaction
    default void refreshData(List<SucursalEntity> sucursales) {
        deleteAll();
        insertAll(sucursales);
    }

}
