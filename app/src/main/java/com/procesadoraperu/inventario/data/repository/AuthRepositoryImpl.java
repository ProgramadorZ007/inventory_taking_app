package com.procesadoraperu.inventario.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.procesadoraperu.inventario.core.utils.JwtDecoder; // NUEVO: Importa tu decodificador
import com.procesadoraperu.inventario.data.local.dao.UsuarioDao;
import com.procesadoraperu.inventario.data.local.entity.UsuarioEntity;
import com.procesadoraperu.inventario.data.remote.api.AuthApi;
import com.procesadoraperu.inventario.data.remote.request.LoginRequest;
import com.procesadoraperu.inventario.data.remote.response.AuthDataResponse;
import com.procesadoraperu.inventario.domain.model.auth.AuthToken;
import com.procesadoraperu.inventario.domain.repository.auth.IAuthRepository;

import org.json.JSONObject; // NUEVO

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

        Response<AuthDataResponse> response = authApi.login(request).execute();

        if (response.isSuccessful() && response.body() != null) {
            AuthDataResponse data = response.body();

            // 1. Guardar el perfil real extrayendo datos del JWT
            UsuarioEntity userEntity = new UsuarioEntity();
            userEntity.username = username;

            // CORRECCIÓN APLICADA: Extraemos los claims
            JSONObject payload = JwtDecoder.decodePayload(data.accessToken);
            if (payload != null) {
                // "http://schemas..." es la ruta estándar de Microsoft IIS/Identity
                userEntity.nombres = payload.optString("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name", "Operario");
                userEntity.idCodigoGeneral = payload.optString("user_code", "");
            } else {
                userEntity.nombres = "Operario (Sin red)";
                userEntity.idCodigoGeneral = "";
            }

            usuarioDao.insert(userEntity);

            // 2. Retornar el token puro para la capa de Dominio
            return new AuthToken(data.accessToken, data.refreshToken, data.expiresIn);
        } else {
            throw new Exception("Credenciales incorrectas o error en el servidor");
        }
    }

    // ... (El resto de tus métodos siguen exactamente igual) ...
    @Override
    public void register(String username, String password, String idCodigoGeneral, String nombres) throws Exception {
        throw new UnsupportedOperationException("El registro desde la app aún no está implementado.");
    }

    @Override
    public AuthToken refreshToken(String refreshToken) throws Exception {
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
        return null;
    }

    @Override
    public boolean isSessionActive() {
        return prefs.getString("ACCESS_TOKEN", null) != null;
    }
}