package com.procesadoraperu.inventario.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.procesadoraperu.inventario.data.local.entity.LogEntity;

import java.util.List;

@Dao
public interface LogDao {

    @Insert
    void insert(LogEntity log);

    // Trae los logs ordenados por fecha, los más nuevos primero
    @Query("SELECT * FROM logs_integracion ORDER BY idLog DESC")
    List<LogEntity> getAllLogs();

    // Cuenta la cantidad total de logs
    @Query("SELECT COUNT(*) FROM logs_integracion")
    int getCount();

    // Elimina el registro más antiguo (el de menor idLog)
    @Query("DELETE FROM logs_integracion WHERE idLog = (SELECT MIN(idLog) FROM logs_integracion)")
    void deleteOldest();

    // Borra todos los logs (ideal para un botón de "Limpiar Caché")
    @Query("DELETE FROM logs_integracion")
    void clearAllLogs();

    /**
     * Inserta un log respetando el límite máximo de 100 registros.
     * Si la tabla ya tiene 100 o más, elimina el más antiguo antes de insertar.
     */
    @Transaction
    default void insertWithLimit(LogEntity log, int maxSize) {
        if (getCount() >= maxSize) {
            deleteOldest();
        }
        insert(log);
    }
}