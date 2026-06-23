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

/**
 * Base de datos SQLite local usando Room.
 * Permite que la app funcione completamente sin conexión a internet.
 *
 * Versión 6: Clave primaria compuesta (idAlmacen, idSucursal) en Almacen
 * para evitar que almacenes de diferentes sucursales se sobreescriban entre sí.
 * fallbackToDestructiveMigration() se usa para sobreescribir copias antiguas en los celulares.
 */
@Database(
        entities = {
                SucursalEntity.class,
                AlmacenEntity.class,
                UsuarioEntity.class,
                ProductoEntity.class,
                InventarioEntity.class,
                LogEntity.class
        },
        version = 6,
        exportSchema = false
)
public abstract class InventarioDatabase extends RoomDatabase {

    // ─── DAOs ────────────────────────────────────────────────
    public abstract SucursalDao sucursalDao();
    public abstract AlmacenDao almacenDao();
    public abstract UsuarioDao usuarioDao();
    public abstract ProductoDao productoDao();
    public abstract InventarioDao inventarioDao();
    public abstract LogDao logDao();

    // ─── Singleton thread-safe ───────────────────────────────
    private static volatile InventarioDatabase INSTANCE;

    public static InventarioDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (InventarioDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    InventarioDatabase.class,
                                    "ppsac_inventario_v6.db"
                            )
                            // Al subir la versión a 6, Room detecta el cambio de esquema
                            // y ejecuta esta línea, destruyendo la vieja v5.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Cierra la instancia (usar solo en tests o limpieza de sesión total).
     */
    public static void destroyInstance() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
        }
        INSTANCE = null;
    }
}