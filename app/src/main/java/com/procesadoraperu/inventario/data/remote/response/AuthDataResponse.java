package com.procesadoraperu.inventario.data.remote.response;

import com.google.gson.annotations.SerializedName;

public class AuthDataResponse {

    @SerializedName("accessToken")
    public String accessToken;

    @SerializedName("refreshToken")
    public String refreshToken;

    @SerializedName("expiresIn")
    public int expiresIn;

    @SerializedName("username")
    public String username;

    @SerializedName("nombres")
    public String nombres;

    @SerializedName("idCodigoGeneral")
    public String idCodigoGeneral;

}
