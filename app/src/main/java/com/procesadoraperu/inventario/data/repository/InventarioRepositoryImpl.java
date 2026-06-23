package com.procesadoraperu.inventario.data.repository;

// Importación de las clases necesarias para acceder a la base de datos local,
// consumir los servicios web, manejar entidades, modelos de dominio y respuestas de la API.
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
import java.util.UUID;

import retrofit2.Response;

/**
 * Implementación del repositorio de Inventario.
 *
 * Esta clase actúa como intermediaria entre la capa de dominio,
 * la base de datos local (Room) y los servicios remotos (API REST).
 *
 * Su responsabilidad principal es gestionar el almacenamiento,
 * consulta y sincronización de inventarios.
 */
public class InventarioRepositoryImpl implements IInventarioRepository {

    /**
     * Identificador fijo de la empresa utilizado cuando
     * el inventario no posee un ID de empresa definido.
     */
    private static final String ID_EMPRESA = "001";

    /**
     * Cliente API utilizado para realizar operaciones remotas.
     */
    private final InventarioApi inventarioApi;

    /**
     * DAO utilizado para acceder a la base de datos local.
     */
    private final InventarioDao inventarioDao;

    /**
     * Constructor que inicializa las dependencias necesarias.
     *
     * @param inventarioApi API para comunicación con el servidor.
     * @param inventarioDao DAO para operaciones locales.
     */
    public InventarioRepositoryImpl(InventarioApi inventarioApi, InventarioDao inventarioDao) {
        this.inventarioApi = inventarioApi;
        this.inventarioDao = inventarioDao;
    }

    /**
     * Envía un inventario al servidor para su registro.
     *
     * Convierte el modelo de dominio en un objeto Request
     * y realiza una petición HTTP mediante Retrofit.
     *
     * @param inventario Inventario a sincronizar.
     * @throws Exception si ocurre un error durante el envío.
     */
    @Override
    public void enviarInventarioRemote(Inventario inventario) throws Exception {
        RegistrarInventarioRequest request = mapToRequest(inventario);

        Response<Void> response =
                inventarioApi.registrarInventario(request).execute();

        if (!response.isSuccessful()) {
            throw new Exception(
                    "Error al sincronizar. Código: " + response.code()
            );
        }
    }

    /**
     * Obtiene desde el servidor el historial de inventarios
     * según sucursal, almacén y rango de fechas.
     *
     * @return Lista de inventarios obtenidos desde la API.
     * @throws Exception si la consulta falla.
     */
    @Override
    public List<Inventario> fetchHistorialRemote(
            String idSucursal,
            String idAlmacen,
            String fechaInicio,
            String fechaFin) throws Exception {

        Response<BaseResponse<List<InventarioEntity>>> response =
                inventarioApi.getHistorial(
                        idSucursal,
                        idAlmacen,
                        fechaInicio,
                        fechaFin
                ).execute();

        if (response.isSuccessful()
                && response.body() != null
                && response.body().isSuccess()) {

            return mapListToDomain(response.body().getData());

        } else {
            throw new Exception(
                    "No se pudo obtener el historial del servidor."
            );
        }
    }

    /**
     * Guarda un inventario en la base de datos local.
     *
     * Primero convierte el modelo de dominio a entidad
     * y posteriormente realiza el insert mediante Room.
     */
    @Override
    public void saveInventarioLocal(Inventario inventario) {
        InventarioEntity entity = mapToEntity(inventario);
        inventarioDao.insert(entity);
    }

    /**
     * Obtiene los inventarios almacenados localmente
     * filtrando por usuario y estado de sincronización.
     *
     * @param username Usuario propietario del registro.
     * @param estado Estado del inventario.
     * @return Lista de inventarios encontrados.
     */
    @Override
    public List<Inventario> getInventariosLocalesPorEstado(
            String username,
            String estado) {

        return mapListToDomain(
                inventarioDao.getByStatusAndUser(username, estado)
        );
    }

    /**
     * Elimina un inventario de la base de datos local.
     *
     * @param inventario Inventario a eliminar.
     */
    @Override
    public void deleteInventarioLocal(Inventario inventario) {
        inventarioDao.delete(mapToEntity(inventario));
    }

    // ==================================================
    // MÉTODOS MAPPER
    // ==================================================
    // Estos métodos permiten transformar los datos entre:
    // Dominio ↔ Entidad ↔ Request
    // para que cada capa trabaje con su propio modelo.

    /**
     * Convierte un objeto de dominio Inventario
     * en un objeto Request para ser enviado a la API.
     */
    private RegistrarInventarioRequest mapToRequest(Inventario d) {

        RegistrarInventarioRequest req =
                new RegistrarInventarioRequest();

        req.idEmpresa =
                (d.getIdEmpresa() != null)
                        ? d.getIdEmpresa()
                        : ID_EMPRESA;

        req.idSucursal = d.getIdSucursal();
        req.idAlmacen = d.getIdAlmacen();
        req.idProducto = d.getIdProducto();
        req.dscProducto = d.getProducto();
        req.idMedida = d.getUnidadMedida();
        req.stock = d.getStock();
        req.cantidad = d.getCantidad();

        // Se copian los datos de auditoría del dispositivo
        // para fines de trazabilidad y seguimiento.
        if (d.getAuditClientInfo() != null) {

            RegistrarInventarioRequest.AuditInfo audit =
                    new RegistrarInventarioRequest.AuditInfo();

            audit.dispositivo =
                    d.getAuditClientInfo().getDispositivo();

            audit.ip =
                    emptyToNull(d.getAuditClientInfo().getIp());

            audit.hostname =
                    d.getAuditClientInfo().getHostname();

            audit.userAgent =
                    d.getAuditClientInfo().getUserAgent();

            // El servidor espera Nullable<Decimal> para lat/lon.
            // Si no se obtuvo ubicación, enviar null en vez de string vacío.
            audit.latitud =
                    emptyToNull(d.getAuditClientInfo().getLatitud());

            audit.longitud =
                    emptyToNull(d.getAuditClientInfo().getLongitud());

            req.auditClientInfo = audit;
        }

        return req;
    }

    /**
     * Convierte un objeto de dominio Inventario
     * en una entidad para almacenamiento local.
     */
    private InventarioEntity mapToEntity(Inventario d) {

        InventarioEntity e = new InventarioEntity();

        // Si el inventario fue creado sin identificador,
        // se genera automáticamente un UUID único.
        if (d.getIdInventario() == null
                || d.getIdInventario().trim().isEmpty()) {

            e.idInventario = UUID.randomUUID().toString();

        } else {
            e.idInventario = d.getIdInventario();
        }

        e.idEmpresa =
                (d.getIdEmpresa() != null)
                        ? d.getIdEmpresa()
                        : ID_EMPRESA;

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

        // Conversión de la información de auditoría.
        if (d.getAuditClientInfo() != null) {

            AuditClientInfoEntity audit =
                    new AuditClientInfoEntity();

            audit.dispositivo =
                    d.getAuditClientInfo().getDispositivo();

            audit.ip =
                    d.getAuditClientInfo().getIp();

            audit.hostname =
                    d.getAuditClientInfo().getHostname();

            audit.userAgent =
                    d.getAuditClientInfo().getUserAgent();

            audit.latitud =
                    d.getAuditClientInfo().getLatitud();

            audit.longitud =
                    d.getAuditClientInfo().getLongitud();

            e.auditClientInfo = audit;
        }

        return e;
    }

    /**
     * Convierte una entidad almacenada en la base de datos
     * en un objeto de dominio utilizado por la aplicación.
     */
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

        // Reconstrucción del objeto de auditoría.
        if (e.auditClientInfo != null) {

            d.setAuditClientInfo(
                    new AuditClientInfo(
                            e.auditClientInfo.dispositivo,
                            e.auditClientInfo.ip,
                            e.auditClientInfo.hostname,
                            e.auditClientInfo.userAgent,
                            e.auditClientInfo.latitud,
                            e.auditClientInfo.longitud
                    )
            );
        }

        return d;
    }

    /**
     * Convierte una lista de entidades en una lista
     * de objetos de dominio.
     *
     * Este método evita repetir la conversión
     * elemento por elemento en distintos lugares.
     */
    private List<Inventario> mapListToDomain(
            List<InventarioEntity> entities) {

        List<Inventario> list = new ArrayList<>();

        if (entities != null) {
            for (InventarioEntity e : entities) {
                list.add(mapToDomain(e));
            }
        }

        return list;
    }

    /**
     * Convierte un String vacío a null.
     * Útil para campos que el servidor espera como Nullable<Decimal>:
     * un string vacío ("") no es parseable como número, pero null sí es aceptado.
     */
    private String emptyToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value;
    }
}