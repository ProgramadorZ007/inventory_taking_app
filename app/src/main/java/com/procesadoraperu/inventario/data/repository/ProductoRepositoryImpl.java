package com.procesadoraperu.inventario.data.repository;

import com.procesadoraperu.inventario.data.local.dao.ProductoDao;
import com.procesadoraperu.inventario.data.local.entity.ProductoEntity;
import com.procesadoraperu.inventario.data.remote.api.ProductoApi;
import com.procesadoraperu.inventario.data.remote.request.ProductoStockRequest;
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

        ProductoStockRequest request = new ProductoStockRequest(idSucursal, idAlmacen, idProducto);

        Response<BaseResponse<List<ProductoEntity>>> response = productoApi.getProductoStock(request).execute();

        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
            List<ProductoEntity> lista = response.body().getData();

            if (lista != null && !lista.isEmpty()) {
                ProductoEntity entity = lista.get(0);
                return mapToDomain(entity);
            } else {
                throw new Exception("El producto no se encontró en este almacén.");
            }
        } else {
            throw new Exception("Error al consultar el stock del producto en el servidor.");
        }
    }

    @Override
    public int downloadAndStoreCatalog(String idSucursal, String idAlmacen) throws Exception {
        ProductoStockRequest request = new ProductoStockRequest(idSucursal, idAlmacen, null);
        Response<BaseResponse<List<ProductoEntity>>> response = productoApi.getProductoStock(request).execute();

        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
            List<ProductoEntity> lista = response.body().getData();
            if (lista == null) {
                lista = new ArrayList<>();
            }
            productoDao.refreshCatalogo(lista);
            return lista.size();
        } else {
            throw new Exception("Error al descargar el catálogo de productos del servidor.");
        }
    }

    @Override
    public Producto getProductoLocal(String idProducto) {
        ProductoEntity entity = productoDao.getProducto(idProducto);
        if (entity == null) {
            return null;
        }
        return mapToDomain(entity);
    }

    @Override
    public void clearLocalCatalog() {
        productoDao.deleteAll();
    }

    @Override
    public int getLocalProductCount() {
        return productoDao.getProductCount();
    }

    // ==========================================================
    // MAPPER (Transformación de Capa Data a Capa Domain)
    // ==========================================================

    private Producto mapToDomain(ProductoEntity entity) {
        if (entity == null) return null;

        BigDecimal stock = entity.stock != null ? BigDecimal.valueOf(entity.stock) : BigDecimal.ZERO;
        BigDecimal disponible = entity.disponible != null ? BigDecimal.valueOf(entity.disponible) : BigDecimal.ZERO;

        Producto producto = new Producto(
                entity.idEmpresa, entity.idProducto, entity.descripcion, entity.idMedida,
                entity.idGrupo, entity.grupoDsc, entity.idSubGrupo, entity.subgrupoDsc,
                stock, disponible, entity.ultFecha
        );

        producto.enriquecerDetallesCatalogo(
                entity.nombreComercial, entity.idUbicacion, entity.tipoproducto, entity.propiedad,
                entity.idCultivo, entity.cultivo, entity.idVariedad, entity.variedad, entity.estado
        );

        return producto;
    }
}