package com.procesadoraperu.inventario.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.procesadoraperu.inventario.data.local.entity.AlmacenEntity;

import java.util.List;

@Dao
public interface AlmacenDao {

    @Query("SELECT * FROM Almacen WHERE idSucursal = :idSucursal")
    List<AlmacenEntity> getPorSucursal(String idSucursal);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AlmacenEntity> almacenes);

    @Query("DELETE FROM Almacen WHERE idSucursal = :idSucursal")
    void deletePorSucursal(String idSucursal);

    // Transacción limpia: Borra los viejos de ESA sucursal y mete los nuevos
    @Transaction
    default void refreshData(String idSucursal, List<AlmacenEntity> almacenes) {
        deletePorSucursal(idSucursal);
        insertAll(almacenes);
    }
}