package com.procesadoraperu.inventario.data.local.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.procesadoraperu.inventario.data.local.dao.SucursalDao;
import com.procesadoraperu.inventario.data.local.dao.UsuarioDao;
import com.procesadoraperu.inventario.data.local.dao.AlmacenDao; // 1. Importa el AlmacenDao
import com.procesadoraperu.inventario.data.local.entity.SucursalEntity;
import com.procesadoraperu.inventario.data.local.entity.UsuarioEntity;
import com.procesadoraperu.inventario.data.local.entity.AlmacenEntity; // 2. Importa la entidad

// 3. Agrega AlmacenEntity a la lista y sube la versión a 2 (o el número que siga)
@Database(entities = {SucursalEntity.class, UsuarioEntity.class, AlmacenEntity.class}, version = 2, exportSchema = false)
public abstract class InventarioDatabase extends RoomDatabase {

    public abstract SucursalDao sucursalDao();
    public abstract UsuarioDao usuarioDao();

    // 4. ESTA ES LA LÍNEA QUE TE FALTA: Expone el AlmacenDao a la base de datos
    public abstract AlmacenDao almacenDao();

    private static volatile InventarioDatabase INSTANCE;

    public static InventarioDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (InventarioDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    InventarioDatabase.class, "ppsac_inventario_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}