package com.procesadoraperu.inventario.data.remote.api;

import com.procesadoraperu.inventario.data.local.entity.InventarioEntity;
import com.procesadoraperu.inventario.data.remote.request.RegistrarInventarioRequest;
import com.procesadoraperu.inventario.data.remote.response.BaseResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface InventarioApi {

    @POST("/api/almacen/inventarios")
    Call<Void> registrarInventario(@Body RegistrarInventarioRequest request);

    @GET("/api/almacen/inventarios")
    Call<BaseResponse<List<InventarioEntity>>> getHistorial(
            @Query("idSucursal") String idSucursal,
            @Query("idAlmacen") String idAlmacen,
            @Query("fechaInicio") String fechaInicio,
            @Query("fechaFin") String fechaFin
    );
}