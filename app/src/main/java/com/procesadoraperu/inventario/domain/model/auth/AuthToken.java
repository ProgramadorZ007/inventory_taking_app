package com.procesadoraperu.inventario.domain.model.auth;

public class AuthToken {

    private final String accessToken;
    private final String refreshToken;
    private final int expiresIn;

    public AuthToken(String accessToken, String refreshToken, int expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public int getExpiresIn() { return expiresIn; }

    // Método de ayuda para la cabecera HTTP
    public String getAuthorizationHeader() {
        return "Bearer " + accessToken;
    }

}
