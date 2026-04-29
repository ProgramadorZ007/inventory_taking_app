package com.procesadoraperu.inventario.domain.repository.inventario;

import com.procesadoraperu.inventario.domain.model.inventario.Inventario;
import java.util.List;

public interface IInventarioRepository {

    // ==========================================
    // OPERACIONES REMOTAS (Nisira API)
    // ==========================================

    /**
     * POST /api/almacen/inventarios
     * Envía el registro de inventario al servidor.
     */
    void enviarInventarioRemote(Inventario inventario) throws Exception;

    /**
     * GET /api/almacen/inventarios?idSucursal=...&idAlmacen=...&fechaInicio=...&fechaFin=...
     * Trae el historial de inventarios desde el servidor.
     */
    List<Inventario> fetchHistorialRemote(String idSucursal, String idAlmacen, String fechaInicio, String fechaFin) throws Exception;

    // ==========================================
    // OPERACIONES LOCALES (SQLite / Room)
    // ==========================================

    /**
     * Guarda un inventario en SQLite.
     * Se usa cuando no hay internet y el estado es "PENDIENTE".
     */
    void saveInventarioLocal(Inventario inventario);

    /**
     * Obtiene los inventarios de SQLite que tienen un estado y usuario específico.
     */
    List<Inventario> getInventariosLocalesPorEstado(String username, String estado);

    /**
     * Elimina un inventario local (Se usa después de sincronizarlo con éxito).
     */
    void deleteInventarioLocal(Inventario inventario);
}