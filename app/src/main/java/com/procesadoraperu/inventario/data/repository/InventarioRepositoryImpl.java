package com.procesadoraperu.inventario.data.repository;

import com.procesadoraperu.inventario.data.local.dao.InventarioDao;
import com.procesadoraperu.inventario.data.local.entity.AuditClientInfoEntity;
import com.procesadoraperu.inventario.data.local.entity.InventarioEntity;
import com.procesadoraperu.inventario.data.remote.api.InventarioApi;
import com.procesadoraperu.inventario.data.remote.request.RegistrarInventarioRequest;
import com.procesadoraperu.inventario.data.remote.response.BaseResponse;
import com.procesadoraperu.inventario.domain.model.inventario.AuditClientInfo;
import com.procesadoraperu.inventario.domain.model.inventario.Inventario;
import com.procesadoraperu.inventario.domain.repository.inventario.IInventarioRepository;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class InventarioRepositoryImpl implements IInventarioRepository {

    private static final String ID_EMPRESA = "001"; // Constante de empresa

    private final InventarioApi inventarioApi;
    private final InventarioDao inventarioDao;

    public InventarioRepositoryImpl(InventarioApi inventarioApi, InventarioDao inventarioDao) {
        this.inventarioApi = inventarioApi;
        this.inventarioDao = inventarioDao;
    }

    @Override
    public void enviarInventarioRemote(Inventario inventario) throws Exception {
        RegistrarInventarioRequest request = mapToRequest(inventario);
        Response<Void> response = inventarioApi.registrarInventario(request).execute();

        if (!response.isSuccessful()) {
            throw new Exception("Error al sincronizar. Código: " + response.code());
        }
    }

    @Override
    public List<Inventario> fetchHistorialRemote(String idSucursal, String idAlmacen,
                                                 String fechaInicio, String fechaFin) throws Exception {
        Response<BaseResponse<List<InventarioEntity>>> response =
                inventarioApi.getHistorial(idSucursal, idAlmacen, fechaInicio, fechaFin).execute();

        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
            return mapListToDomain(response.body().getData());
        } else {
            throw new Exception("No se pudo obtener el historial del servidor.");
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
    // MAPPERS
    // ==========================================

    private RegistrarInventarioRequest mapToRequest(Inventario d) {
        RegistrarInventarioRequest req = new RegistrarInventarioRequest();
        req.idEmpresa   = (d.getIdEmpresa() != null) ? d.getIdEmpresa() : ID_EMPRESA;
        req.idSucursal  = d.getIdSucursal();
        req.idAlmacen   = d.getIdAlmacen();
        req.idProducto  = d.getIdProducto();
        req.dscProducto = d.getProducto();
        req.idMedida    = d.getUnidadMedida();
        req.stock       = d.getStock();
        req.cantidad    = d.getCantidad();

        if (d.getAuditClientInfo() != null) {
            RegistrarInventarioRequest.AuditInfo audit = new RegistrarInventarioRequest.AuditInfo();
            audit.dispositivo = d.getAuditClientInfo().getDispositivo();
            audit.ip          = d.getAuditClientInfo().getIp();
            audit.hostname    = d.getAuditClientInfo().getHostname();
            audit.userAgent   = d.getAuditClientInfo().getUserAgent();

            // CORRECCIÓN AQUÍ: Evitamos enviar textos vacíos a la API
            String lat = d.getAuditClientInfo().getLatitud();
            String lon = d.getAuditClientInfo().getLongitud();

            audit.latitud = (lat != null && !lat.trim().isEmpty()) ? lat : null;
            audit.longitud = (lon != null && !lon.trim().isEmpty()) ? lon : null;

            req.auditClientInfo = audit;
        }
        return req;
    }

    private InventarioEntity mapToEntity(Inventario d) {
        InventarioEntity e = new InventarioEntity();
        e.idInventario         = d.getIdInventario();
        e.idEmpresa            = (d.getIdEmpresa() != null) ? d.getIdEmpresa() : ID_EMPRESA;
        e.idSucursal           = d.getIdSucursal();
        e.sucursal             = d.getSucursal();
        e.idAlmacen            = d.getIdAlmacen();
        e.almacen              = d.getAlmacen();
        e.idProducto           = d.getIdProducto();
        e.producto             = d.getProducto();
        e.unidadMedida         = d.getUnidadMedida();
        e.stock                = d.getStock();
        e.cantidad             = d.getCantidad();
        e.usuarioCreacion      = d.getUsuarioCreacion();
        e.fechaCreacion        = d.getFechaCreacion();
        e.fechaRegistroLocal   = d.getFechaRegistroLocal();
        e.estadoSincronizacion = d.getEstadoSincronizacion();

        if (d.getAuditClientInfo() != null) {
            AuditClientInfoEntity audit = new AuditClientInfoEntity();
            audit.dispositivo = d.getAuditClientInfo().getDispositivo();
            audit.ip          = d.getAuditClientInfo().getIp();
            audit.hostname    = d.getAuditClientInfo().getHostname();
            audit.userAgent   = d.getAuditClientInfo().getUserAgent();
            audit.latitud     = d.getAuditClientInfo().getLatitud();
            audit.longitud    = d.getAuditClientInfo().getLongitud();
            e.auditClientInfo = audit;
        }
        return e;
    }

    private Inventario mapToDomain(InventarioEntity e) {
        Inventario d = new Inventario();
        d.setIdInventario(e.idInventario);
        d.setIdEmpresa(e.idEmpresa);
        d.setIdSucursal(e.idSucursal);
        d.setSucursal(e.sucursal);
        d.setIdAlmacen(e.idAlmacen);
        d.setAlmacen(e.almacen);
        d.setIdProducto(e.idProducto);
        d.setProducto(e.producto);
        d.setUnidadMedida(e.unidadMedida);
        d.setStock(e.stock);
        d.setCantidad(e.cantidad);
        d.setUsuarioCreacion(e.usuarioCreacion);
        d.setFechaCreacion(e.fechaCreacion);
        d.setFechaRegistroLocal(e.fechaRegistroLocal);
        d.setEstadoSincronizacion(e.estadoSincronizacion);

        if (e.auditClientInfo != null) {
            d.setAuditClientInfo(new AuditClientInfo(
                    e.auditClientInfo.dispositivo,
                    e.auditClientInfo.ip,
                    e.auditClientInfo.hostname,
                    e.auditClientInfo.userAgent,
                    e.auditClientInfo.latitud,
                    e.auditClientInfo.longitud
            ));
        }
        return d;
    }

    private List<Inventario> mapListToDomain(List<InventarioEntity> entities) {
        List<Inventario> list = new ArrayList<>();
        if (entities != null) {
            for (InventarioEntity e : entities) {
                list.add(mapToDomain(e));
            }
        }
        return list;
    }
}