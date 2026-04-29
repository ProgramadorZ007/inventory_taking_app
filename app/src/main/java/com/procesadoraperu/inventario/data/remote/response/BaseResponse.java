package com.procesadoraperu.inventario.data.remote.response;

import com.google.gson.annotations.SerializedName;

public class BaseResponse<T> {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private T data; // Aquí puede ir una Lista o un Objeto individual

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }

}
