package com.procesadoraperu.inventario.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.procesadoraperu.inventario.core.network.ApiClient;
import com.procesadoraperu.inventario.core.utils.JwtDecoder;
import com.procesadoraperu.inventario.data.local.dao.UsuarioDao;
import com.procesadoraperu.inventario.data.local.entity.UsuarioEntity;
import com.procesadoraperu.inventario.data.remote.api.AuthApi;
import com.procesadoraperu.inventario.data.remote.request.LoginRequest;
import com.procesadoraperu.inventario.data.remote.response.AuthDataResponse;
import com.procesadoraperu.inventario.domain.model.auth.AuthToken;
import com.procesadoraperu.inventario.domain.repository.auth.IAuthRepository;

import org.json.JSONObject;

import retrofit2.Response;

public class AuthRepositoryImpl implements IAuthRepository {

    private final AuthApi authApi;
    private final UsuarioDao usuarioDao;
    private final SharedPreferences prefs;
    private final SharedPreferences appPrefs;

    public AuthRepositoryImpl(AuthApi authApi, UsuarioDao usuarioDao, Context context) {
        this.authApi = authApi;
        this.usuarioDao = usuarioDao;
        this.prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        this.appPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
    }

    @Override
    public AuthToken login(String username, String password) throws Exception {
        LoginRequest request = new LoginRequest(username, password);

        Response<AuthDataResponse> response = authApi.login(request).execute();

        if (response.isSuccessful() && response.body() != null) {
            AuthDataResponse data = response.body();

            // Validar que el accessToken no sea nulo antes de continuar
            if (data.accessToken == null || data.accessToken.isEmpty()) {
                throw new Exception("El servidor no devolvió un token válido.");
            }

            // Validar que el refreshToken no sea nulo para evitar fallos en renovación futura
            if (data.refreshToken == null || data.refreshToken.isEmpty()) {
                Log.w("AuthRepositoryImpl", "El servidor no devolvió un refreshToken. La sesión no se podrá renovar automáticamente.");
            }

            // Guardar el perfil del usuario
            UsuarioEntity userEntity = new UsuarioEntity();
            // CORRECCIÓN: Guardar siempre el username en minúsculas para evitar
            // inconsistencias al comparar en el historial (equalsIgnoreCase)
            userEntity.username = username.trim().toLowerCase();

            JSONObject payload = JwtDecoder.decodePayload(data.accessToken);
            if (payload != null) {
                userEntity.nombres = payload.optString(
                        "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name",
                        username // Fallback al username si no hay nombre en el token
                );
                userEntity.idCodigoGeneral = payload.optString("user_code", "");
            } else {
                // CORRECCIÓN: Si el JWT no se puede leer, usamos el nombre que vino
                // directamente en la respuesta del login (campo 'nombres')
                userEntity.nombres = (data.nombres != null && !data.nombres.isEmpty())
                        ? data.nombres : username;
                userEntity.idCodigoGeneral = (data.idCodigoGeneral != null)
                        ? data.idCodigoGeneral : "";
            }

            // CORRECCIÓN: Limpiar usuario anterior antes de insertar el nuevo
            usuarioDao.deleteUsuario();
            usuarioDao.insert(userEntity);

            return new AuthToken(data.accessToken, data.refreshToken, data.expiresIn);

        } else {
            // CORRECCIÓN: Extraer mensaje de error del servidor si está disponible
            String errorBody = "";
            if (response.errorBody() != null) {
                try {
                    errorBody = response.errorBody().string();
                } catch (Exception ignored) {}
            }

            if (response.code() == 401) {
                throw new Exception("Usuario o contraseña incorrectos.");
            } else if (response.code() == 0) {
                throw new Exception("Sin conexión al servidor. Verifica tu red.");
            } else {
                throw new Exception("Error del servidor (" + response.code() + ")."
                        + (errorBody.isEmpty() ? "" : " " + errorBody));
            }
        }
    }

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
                .putLong("SESSION_SAVED_AT", System.currentTimeMillis())
                .apply();
    }

    @Override
    public void logout() {
        // Limpiar credenciales de SharedPreferences
        prefs.edit().clear().apply();

        // Limpiar ubicación activa (sucursal/almacén) para forzar selección en próximo login
        appPrefs.edit().clear().apply();

        // Limpiar perfil de usuario de la BD local
        usuarioDao.deleteUsuario();

        // CORRECCIÓN: Resetear el singleton de Retrofit para que el próximo
        // login construya un cliente HTTP limpio (sin tokens viejos en caché)
        ApiClient.reset();
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
        String accessToken = prefs.getString("ACCESS_TOKEN", null);
        if (accessToken == null) {
            return false;
        }

        // Verificar si el access token ha expirado
        if (JwtDecoder.isTokenExpired(accessToken)) {
            // El access token expiró, pero si hay refresh token aún podemos renovar
            String refreshToken = prefs.getString("REFRESH_TOKEN", null);
            return refreshToken != null && !refreshToken.isEmpty();
        }

        return true;
    }
}