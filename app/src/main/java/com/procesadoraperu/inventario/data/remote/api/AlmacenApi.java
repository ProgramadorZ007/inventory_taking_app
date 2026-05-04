package com.procesadoraperu.inventario.data.remote.api;

import com.procesadoraperu.inventario.data.local.entity.AlmacenEntity;
import com.procesadoraperu.inventario.data.remote.response.BaseResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path; // Cambiamos Query por Path

public interface AlmacenApi {

    // El valor entre llaves {idSucursal} es un marcador de posición
    @GET("/api/nisira/sucursales/{idSucursal}/almacenes")
    Call<BaseResponse<List<AlmacenEntity>>> getAlmacenes(@Path("idSucursal") String idSucursal);

}