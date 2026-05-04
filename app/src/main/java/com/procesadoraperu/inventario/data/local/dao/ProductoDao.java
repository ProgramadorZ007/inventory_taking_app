package com.procesadoraperu.inventario.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.procesadoraperu.inventario.data.local.entity.ProductoEntity;

import java.util.List;

@Dao
public interface ProductoDao {

    // Buscar un producto específico (para enriquecer el escaneo)
    @Query("SELECT * FROM Producto WHERE idProducto = :idProducto LIMIT 1")
    ProductoEntity getProducto(String idProducto);

    // Guardar el catálogo maestro
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProductoEntity> productos);

    // Borrar catálogo antiguo
    @Query("DELETE FROM Producto")
    void deleteAll();

    // Transacción limpia para sincronizar el catálogo
    @Transaction
    default void refreshCatalogo(List<ProductoEntity> productos) {
        deleteAll();
        insertAll(productos);
    }
}