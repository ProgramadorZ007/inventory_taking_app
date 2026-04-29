package com.procesadoraperu.inventario.domain.repository.almacen;

import com.procesadoraperu.inventario.domain.model.almacen.Almacen;

import java.util.List;

public interface IAlmacenRepository {

    // ==========================================================
    // 1. OPERACIONES DE LISTADO Y SINCRONIZACIÓN (GetAlmacenesUseCase)
    // ==========================================================

    /**
     * Obtiene los almacenes desde el servidor (API Nisira) filtrados por sucursal.
     * @param idSucursal El ID de la sucursal de la cual queremos sus almacenes.
     */
    List<Almacen> fetchAlmacenesRemote(String idSucursal) throws Exception;

    /**
     * Obtiene los almacenes almacenados localmente en SQLite (Offline).
     * @param idSucursal El ID de la sucursal para filtrar en la base de datos local.
     */
    List<Almacen> getAlmacenesLocal(String idSucursal);

    /**
     * Guarda los almacenes obtenidos de la API en la base de datos local (Room).
     * Idealmente, el Repositorio Impl debería borrar los almacenes antiguos
     * de esa sucursal antes de insertar los nuevos para evitar duplicados.
     */
    void saveAlmacenesLocal(List<Almacen> almacenes);


    // ==========================================================
    // 2. OPERACIONES DE ESTADO DE SESIÓN (Save/Get ActiveAlmacenUseCase)
    // ==========================================================

    /**
     * Guarda el Almacén completo (ID y Descripción) seleccionado por el operario.
     * Esto permite que las siguientes pantallas sepan dónde se está tomando el inventario.
     */
    void saveActiveAlmacen(Almacen almacen);

    /**
     * Recupera el Almacén en el que el operario está trabajando actualmente.
     * @return El objeto Almacen, o null si el usuario aún no ha seleccionado ninguno.
     */
    Almacen getActiveAlmacen();
}