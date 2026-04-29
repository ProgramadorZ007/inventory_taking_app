package com.procesadoraperu.inventario.data.remote.api;

import com.procesadoraperu.inventario.data.local.entity.SucursalEntity;
import com.procesadoraperu.inventario.data.remote.response.BaseResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface SucursalApi {
    // Usamos SucursalEntity temporalmente como DTO porque sus campos
    // (idSucursal, descripcion) coinciden exacto con el JSON.
    @GET("/api/nisira/sucursales")
    Call<BaseResponse<List<SucursalEntity>>> getSucursales();
}