package com.procesadoraperu.inventario.data.repository;

import com.procesadoraperu.inventario.data.local.dao.ProductoDao;
import com.procesadoraperu.inventario.data.local.entity.ProductoEntity;
import com.procesadoraperu.inventario.data.remote.api.ProductoApi;
import com.procesadoraperu.inventario.data.remote.request.ProductoStockRequest; // ¡CORRECCIÓN: Faltaba este import!
import com.procesadoraperu.inventario.data.remote.response.BaseResponse;
import com.procesadoraperu.inventario.domain.model.producto.Producto;
import com.procesadoraperu.inventario.domain.repository.producto.IProductoRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class ProductoRepositoryImpl implements IProductoRepository {

    private final ProductoApi productoApi;
    private final ProductoDao productoDao;

    public ProductoRepositoryImpl(ProductoApi productoApi, ProductoDao productoDao) {
        this.productoApi = productoApi;
        this.productoDao = productoDao;
    }

    @Override
    public Producto fetchProductoStock(String idSucursal, String idAlmacen, String idProducto) throws Exception {

        // 1. Empaquetamos las 3 variables en el Request
        ProductoStockRequest request = new ProductoStockRequest(idSucursal, idAlmacen, idProducto);

        // 2. Ejecutamos la petición POST
        Response<BaseResponse<List<ProductoEntity>>> response = productoApi.getProductoStock(request).execute();

        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
            List<ProductoEntity> lista = response.body().getData();
            if (lista != null && !lista.isEmpty()) {
                return mapToDomain(lista.get(0));
            }
            throw new Exception("Producto no encontrado.");
        } else {
            throw new Exception("Error al consultar el stock del producto en el servidor.");
        }
    }

    @Override
    public List<Producto> fetchAllProductosRemote(String idGrupo, String idSubGrupo) throws Exception {
        // Si idGrupo o idSubGrupo son null, Retrofit no los incluirá en la URL
        Response<BaseResponse<List<ProductoEntity>>> response =
                productoApi.getAllProductos(idGrupo, idSubGrupo).execute();

        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
            List<ProductoEntity> entities = response.body().getData();
            List<Producto> productos = new ArrayList<>();
            for (ProductoEntity e : entities) {
                productos.add(mapToDomain(e));
            }
            return productos;
        } else {
            throw new Exception("Error al descargar el catálogo de productos.");
        }
    }

    @Override
    public void saveProductosLocal(List<Producto> productos) {
        if (productos == null || productos.isEmpty()) return;

        List<ProductoEntity> entities = new ArrayList<>();
        for (Producto p : productos) {
            entities.add(mapToEntity(p));
        }
        // Guardamos todo en SQLite reemplazando lo viejo
        productoDao.refreshCatalogo(entities);
    }

    @Override
    public Producto getProductoLocal(String idProducto) {
        ProductoEntity entity = productoDao.getProducto(idProducto);
        return mapToDomain(entity);
    }

    // ==========================================================
    // MAPPERS (Transformación entre Capa Data y Capa Domain)
    // ==========================================================

    private Producto mapToDomain(ProductoEntity entity) {
        if (entity == null) return null;

        // 1. Convertimos los Doubles a BigDecimal de forma segura
        BigDecimal stock = entity.stock != null ? BigDecimal.valueOf(entity.stock) : BigDecimal.ZERO;
        BigDecimal disponible = entity.disponible != null ? BigDecimal.valueOf(entity.disponible) : BigDecimal.ZERO;

        // 2. Instanciamos el modelo de dominio usando tu constructor del Paso 1
        Producto producto = new Producto(
                entity.idEmpresa, entity.idProducto, entity.descripcion, entity.idMedida,
                entity.idGrupo, entity.grupoDsc, entity.idSubGrupo, entity.subgrupoDsc,
                stock, disponible, entity.ultFecha
        );

        // 3. Le inyectamos los datos del catálogo usando tu método del Paso 2
        producto.enriquecerDetallesCatalogo(
                entity.nombreComercial, entity.idUbicacion, entity.tipoproducto, entity.propiedad,
                entity.idCultivo, entity.cultivo, entity.idVariedad, entity.variedad, entity.estado
        );

        return producto;
    }

    private ProductoEntity mapToEntity(Producto p) {
        if (p == null) return null;

        ProductoEntity entity = new ProductoEntity();
        entity.idEmpresa = p.getIdEmpresa();
        entity.idProducto = p.getIdProducto();
        entity.descripcion = p.getDescripcion();
        entity.idMedida = p.getIdMedida();
        entity.idGrupo = p.getIdGrupo();
        entity.grupoDsc = p.getGrupo();
        entity.idSubGrupo = p.getIdSubGrupo();
        entity.subgrupoDsc = p.getSubGrupo();
        entity.ultFecha = p.getUltFecha();

        entity.stock = p.getStock().doubleValue();
        entity.disponible = p.getDisponible().doubleValue();

        entity.nombreComercial = p.getNombreComercial();
        entity.idUbicacion = p.getIdUbicacion();
        entity.tipoproducto = p.getTipoproducto();
        entity.propiedad = p.getPropiedad();
        entity.idCultivo = p.getIdCultivo();
        entity.cultivo = p.getCultivo();
        entity.idVariedad = p.getIdVariedad();
        entity.variedad = p.getVariedad();
        entity.estado = p.getEstado();

        return entity;
    }
}