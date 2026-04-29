package com.procesadoraperu.inventario.domain.repository.sucursal;

import com.procesadoraperu.inventario.domain.model.sucursal.Sucursal;

import java.util.List;

public interface ISucursalRepository {

    // ==========================================================
    // 1. OPERACIONES DE LISTADO (Para GetSucursalesUseCase)
    // ==========================================================

    /**
     * Obtiene la lista de sucursales desde el servidor (API Nisira).
     * Se usa para la carga inicial de maestros o actualización forzada.
     */
    List<Sucursal> fetchSucursalesRemote() throws Exception;

    /**
     * Obtiene las sucursales almacenadas localmente en SQLite.
     * Permite que la app funcione sin internet tras la primera carga.
     */
    List<Sucursal> getSucursalesLocal();

    /**
     * Guarda las sucursales obtenidas de la API en la base de datos local (Room).
     */
    void saveSucursalesLocal(List<Sucursal> sucursales);

    // ==========================================================
    // 2. OPERACIONES DE ESTADO (Para Save/Get ActiveSucursalUseCase)
    // ==========================================================

    /**
     * Guarda el ID de la sucursal seleccionada por el operario.
     * Generalmente se implementa usando SharedPreferences.
     */
    void saveActiveSucursalId(String idSucursal);

    /**
     * Recupera el ID de la sucursal previamente seleccionada.
     * Retorna null o vacío si aún no se ha seleccionado ninguna.
     */
    String getActiveSucursalId();
}