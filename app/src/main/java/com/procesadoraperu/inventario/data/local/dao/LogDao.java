package com.procesadoraperu.inventario.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.procesadoraperu.inventario.data.local.entity.LogEntity;

import java.util.List;

@Dao
public interface LogDao {

    @Insert
    void insert(LogEntity log);

    // Trae los logs ordenados por fecha, los más nuevos primero
    @Query("SELECT * FROM logs_integracion ORDER BY idLog DESC")
    List<LogEntity> getAllLogs();

    // Borra todos los logs (ideal para un botón de "Limpiar Caché")
    @Query("DELETE FROM logs_integracion")
    void clearAllLogs();
}