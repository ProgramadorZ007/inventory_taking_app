package com.procesadoraperu.inventario.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.procesadoraperu.inventario.data.local.dao.UsuarioDao;
import com.procesadoraperu.inventario.data.local.entity.UsuarioEntity;
import com.procesadoraperu.inventario.data.remote.api.AuthApi;
import com.procesadoraperu.inventario.data.remote.request.LoginRequest;
import com.procesadoraperu.inventario.data.remote.response.AuthDataResponse;
import com.procesadoraperu.inventario.data.remote.response.BaseResponse;
import com.procesadoraperu.inventario.domain.model.auth.AuthToken;
import com.procesadoraperu.inventario.domain.repository.auth.IAuthRepository;

import retrofit2.Response;

public class AuthRepositoryImpl implements IAuthRepository {

    private final AuthApi authApi;
    private final UsuarioDao usuarioDao;
    private final SharedPreferences prefs;

    public AuthRepositoryImpl(AuthApi authApi, UsuarioDao usuarioDao, Context context) {
        this.authApi = authApi;
        this.usuarioDao = usuarioDao;
        this.prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
    }

    @Override
    public AuthToken login(String username, String password) throws Exception {
        LoginRequest request = new LoginRequest(username, password);

        // Ya no usamos BaseResponse aquí tampoco
        Response<AuthDataResponse> response = authApi.login(request).execute();

        // Verificamos que sea HTTP 200 y que el cuerpo no esté vacío
        if (response.isSuccessful() && response.body() != null) {
            AuthDataResponse data = response.body();

            // 1. Guardar perfil del usuario en SQLite (Room)
            UsuarioEntity userEntity = new UsuarioEntity();
            userEntity.username = username; // Usamos el que digitó en pantalla
            userEntity.nombres = "Operario"; // Placeholder temporal
            userEntity.idCodigoGeneral = "";
            usuarioDao.insert(userEntity);

            // 2. Retornar el token puro para la capa de Dominio
            return new AuthToken(data.accessToken, data.refreshToken, data.expiresIn);
        } else {
            throw new Exception("Credenciales incorrectas o error en el servidor");
        }
    }

    @Override
    public void register(String username, String password, String idCodigoGeneral, String nombres) throws Exception {
        // TODO: Implementar consumo del endpoint /api/auth/register en AuthApi
        throw new UnsupportedOperationException("El registro desde la app aún no está implementado.");
    }

    @Override
    public AuthToken refreshToken(String refreshToken) throws Exception {
        // TODO: Implementar consumo del endpoint /api/auth/refresh-token en AuthApi
        throw new UnsupportedOperationException("La renovación de token aún no está implementada.");
    }

    @Override
    public void saveSession(AuthToken token) {
        prefs.edit()
                .putString("ACCESS_TOKEN", token.getAccessToken())
                .putString("REFRESH_TOKEN", token.getRefreshToken())
                .putInt("EXPIRES_IN", token.getExpiresIn())
                .apply();
    }

    @Override
    public void logout() {
        // Borramos las preferencias (tokens) y la base de datos del usuario
        prefs.edit().clear().apply();
        usuarioDao.deleteUsuario();
    }

    @Override
    public AuthToken getSession() {
        String accessToken = prefs.getString("ACCESS_TOKEN", null);
        String refreshToken = prefs.getString("REFRESH_TOKEN", null);
        int expiresIn = prefs.getInt("EXPIRES_IN", 0);

        if (accessToken != null && refreshToken != null) {
            return new AuthToken(accessToken, refreshToken, expiresIn);
        }
        return null; // No hay sesión guardada
    }

    @Override
    public boolean isSessionActive() {
        return prefs.getString("ACCESS_TOKEN", null) != null;
    }
}