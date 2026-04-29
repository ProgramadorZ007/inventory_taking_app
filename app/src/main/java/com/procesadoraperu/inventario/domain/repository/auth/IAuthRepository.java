package com.procesadoraperu.inventario.domain.repository.auth;

import com.procesadoraperu.inventario.domain.model.auth.AuthToken;

public interface IAuthRepository {

    /**
     * Autentica al usuario en el ERP y devuelve los tokens JWT.
     * Endpoint: POST /api/auth/login
     */
    AuthToken login(String username, String password) throws Exception;

    /**
     * Registra un nuevo operario en el sistema.
     * Endpoint: POST /api/auth/register
     */
    void register(String username, String password, String idCodigoGeneral, String nombres) throws Exception;

    /**
     * Renueva el Access Token usando el Refresh Token almacenado.
     * Endpoint: POST /api/auth/refresh-token
     */
    AuthToken refreshToken(String refreshToken) throws Exception;

    /**
     * Invalida la sesión actual en el servidor y limpia datos locales.
     * Endpoint: POST /api/auth/logout
     */
    void logout();

    /**
     * Guarda el token de forma local para mantener la sesión iniciada.
     */
    void saveSession(AuthToken token);

    /**
     * Recupera el token guardado para inyectarlo en las peticiones.
     */
    AuthToken getSession();

    /**
     * Verifica si existe una sesión activa y válida en el almacenamiento local.
     */
    boolean isSessionActive();
}