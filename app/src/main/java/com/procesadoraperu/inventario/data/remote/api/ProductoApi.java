package com.procesadoraperu.inventario.data.remote.api;

import com.procesadoraperu.inventario.data.local.entity.ProductoEntity;
import com.procesadoraperu.inventario.data.remote.request.ProductoStockRequest;
import com.procesadoraperu.inventario.data.remote.response.BaseResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query; // Importante

public interface    ProductoApi {

    @POST("/api/nisira/producto-stock")
    Call<BaseResponse<List<ProductoEntity>>> getProductoStock(@Body ProductoStockRequest request);

    // CORREGIDO: GET con parámetros de consulta (?idGrupoPro=...&idSubGrupoPro=...)
    @GET("/api/nisira/productos")
    Call<BaseResponse<List<ProductoEntity>>> getAllProductos(
            @Query("idGrupoPro") String idGrupo,
            @Query("idSubGrupoPro") String idSubGrupo
    );
}