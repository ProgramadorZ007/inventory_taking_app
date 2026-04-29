package com.procesadoraperu.inventario.data.local.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.procesadoraperu.inventario.data.local.dao.SucursalDao;
import com.procesadoraperu.inventario.data.local.dao.UsuarioDao;
import com.procesadoraperu.inventario.data.local.entity.SucursalEntity;
import com.procesadoraperu.inventario.data.local.entity.UsuarioEntity;

// Incrementa la versión si cambias la estructura de alguna tabla en el futuro
@Database(entities = {SucursalEntity.class, UsuarioEntity.class}, version = 1, exportSchema = false)
public abstract class InventarioDatabase extends RoomDatabase {

    // Exponer los DAOs
    public abstract SucursalDao sucursalDao();
    public abstract UsuarioDao usuarioDao();

    // Patrón Singleton para evitar abrir múltiples instancias de la BD
    private static volatile InventarioDatabase INSTANCE;

    public static InventarioDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (InventarioDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    InventarioDatabase.class, "ppsac_inventario_db")
                            .fallbackToDestructiveMigration() // Borra la DB si subes versión (Ideal para desarrollo)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}