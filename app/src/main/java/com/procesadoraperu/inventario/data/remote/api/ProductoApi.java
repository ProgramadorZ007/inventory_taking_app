package com.procesadoraperu.inventario.data.remote.api;

import com.procesadoraperu.inventario.data.local.entity.ProductoEntity;
import com.procesadoraperu.inventario.data.remote.request.ProductoStockRequest;
import com.procesadoraperu.inventario.data.remote.response.BaseResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ProductoApi {

    @POST("/api/nisira/producto-stock")
    Call<BaseResponse<List<ProductoEntity>>> getProductoStock(@Body ProductoStockRequest request);
}