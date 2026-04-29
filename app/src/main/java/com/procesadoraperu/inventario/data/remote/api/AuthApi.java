package com.procesadoraperu.inventario.data.remote.api;

import com.procesadoraperu.inventario.data.remote.request.LoginRequest;
import com.procesadoraperu.inventario.data.remote.response.AuthDataResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {
    @POST("/api/auth/login")
        // ATENCIÓN: Quitamos el BaseResponse, ahora es directo
    Call<AuthDataResponse> login(@Body LoginRequest request);
}