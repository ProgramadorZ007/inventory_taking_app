package com.procesadoraperu.inventario.data.repository;

import com.procesadoraperu.inventario.data.local.dao.InventarioDao;
import com.procesadoraperu.inventario.data.local.entity.InventarioEntity;
import com.procesadoraperu.inventario.data.remote.api.InventarioApi;
import com.procesadoraperu.inventario.data.remote.response.BaseResponse;
import com.procesadoraperu.inventario.domain.model.inventario.AuditClientInfo;
import com.procesadoraperu.inventario.domain.model.inventario.Inventario;
import com.procesadoraperu.inventario.domain.repository.inventario.IInventarioRepository;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Response;

public class InventarioRepositoryImpl implements IInventarioRepository {

    private final InventarioApi inventarioApi;
    private final InventarioDao inventarioDao;

    public InventarioRepositoryImpl(InventarioApi inventarioApi, InventarioDao inventarioDao) {
        this.inventarioApi = inventarioApi;
        this.inventarioDao = inventarioDao;
    }

    @Override
    public void enviarInventarioRemote(Inventario inventario) throws Exception {
        InventarioEntity entity = mapToEntity(inventario);
        Response<Void> response = inventarioApi.registrarInventario(entity).execute();

        if (!response.isSuccessful()) {
            throw new Exception("Error al sincronizar con el servidor de Procesadora Perú.");
        }
    }

    @Override
    public List<Inventario> fetchHistorialRemote(String idSucursal, String idAlmacen, String fechaInicio, String fechaFin) throws Exception {
        Response<BaseResponse<List<InventarioEntity>>> response =
                inventarioApi.getHistorial(idSucursal, idAlmacen, fechaInicio, fechaFin).execute();

        if (response.isSuccessful() && response.body() != null) {
            return mapListToDomain(response.body().getData());
        } else {
            throw new Exception("No se pudo obtener el historial.");
        }
    }

    @Override
    public void saveInventarioLocal(Inventario inventario) {
        inventarioDao.insert(mapToEntity(inventario));
    }

    @Override
    public List<Inventario> getInventariosLocalesPorEstado(String username, String estado) {
        return mapListToDomain(inventarioDao.getByStatusAndUser(username, estado));
    }

    @Override
    public void deleteInventarioLocal(Inventario inventario) {
        inventarioDao.delete(mapToEntity(inventario));
    }

    // ==========================================
    // MAPPERS (Transformación de Datos)
    // ==========================================

    private InventarioEntity mapToEntity(Inventario d) {
        InventarioEntity e = new InventarioEntity();
        e.idInventario = d.getIdInventario();
        e.idEmpresa = d.getIdEmpresa();
        e.idSucursal = d.getIdSucursal();
        e.sucursal = d.getSucursal();
        e.idAlmacen = d.getIdAlmacen();
        e.almacen = d.getAlmacen();
        e.idProducto = d.getIdProducto();
        e.producto = d.getProducto();
        e.unidadMedida = d.getUnidadMedida();
        e.stock = d.getStock();
        e.cantidad = d.getCantidad();
        e.usuarioCreacion = d.getUsuarioCreacion();
        e.fechaCreacion = d.getFechaCreacion();
        e.fechaRegistroLocal = d.getFechaRegistroLocal();
        e.estadoSincronizacion = d.getEstadoSincronizacion();

        if (d.getAuditClientInfo() != null) {
            // Mapeo manual del objeto embebido
            // ... (rellenar campos de AuditClientInfoEntity)
        }
        return e;
    }

    private List<Inventario> mapListToDomain(List<InventarioEntity> entities) {
        List<Inventario> list = new ArrayList<>();
        if (entities != null) {
            for (InventarioEntity e : entities) {
                Inventario i = new Inventario();
                // ... (setear campos de i desde e)
                list.add(i);
            }
        }
        return list;
    }
}