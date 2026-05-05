package com.procesadoraperu.inventario.data.local.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.procesadoraperu.inventario.data.local.dao.AlmacenDao;
import com.procesadoraperu.inventario.data.local.dao.InventarioDao;
import com.procesadoraperu.inventario.data.local.dao.LogDao;
import com.procesadoraperu.inventario.data.local.dao.ProductoDao;
import com.procesadoraperu.inventario.data.local.dao.SucursalDao;
import com.procesadoraperu.inventario.data.local.dao.UsuarioDao;
import com.procesadoraperu.inventario.data.local.entity.AlmacenEntity;
import com.procesadoraperu.inventario.data.local.entity.InventarioEntity;
import com.procesadoraperu.inventario.data.local.entity.LogEntity;
import com.procesadoraperu.inventario.data.local.entity.ProductoEntity;
import com.procesadoraperu.inventario.data.local.entity.SucursalEntity;
import com.procesadoraperu.inventario.data.local.entity.UsuarioEntity;

@Database(
        entities = {
                SucursalEntity.class,
                UsuarioEntity.class,
                AlmacenEntity.class,
                ProductoEntity.class,
                InventarioEntity.class,
                LogEntity.class
        },
        version = 3,
        exportSchema = false
)
public abstract class InventarioDatabase extends RoomDatabase {

    public abstract SucursalDao sucursalDao();
    public abstract UsuarioDao usuarioDao();
    public abstract AlmacenDao almacenDao();
    public abstract ProductoDao productoDao();
    public abstract InventarioDao inventarioDao();
    public abstract LogDao logDao();

    private static volatile InventarioDatabase INSTANCE;

    public static InventarioDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (InventarioDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    InventarioDatabase.class,
                                    "ppsac_inventario_db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}