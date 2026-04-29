package com.procesadoraperu.inventario.domain.usecase.auth;

import com.procesadoraperu.inventario.domain.model.auth.AuthToken;
import com.procesadoraperu.inventario.domain.repository.auth.IAuthRepository;

public class LoginUseCase {

    private final IAuthRepository _authRepository;

    // Inyección de dependencias: El caso de uso no sabe CÓMO se hace el login,
    // solo sabe a QUIÉN pedírselo.
    public LoginUseCase(IAuthRepository authRepository) {
        this._authRepository = authRepository;
    }

    /**
     * Ejecuta el caso de uso del Login.
     * @param username Usuario ingresado en la UI
     * @param password Contraseña ingresada en la UI
     * @return El token de autenticación si es exitoso
     * @throws Exception Si las credenciales son inválidas o no hay red
     */
    public AuthToken execute(String username, String password) throws Exception {
        // 1. Validaciones de negocio simples
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("El usuario no puede estar vacío");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }

        // 2. Ejecutar la llamada al repositorio (API Nisira)
        AuthToken token = _authRepository.login(username, password);

        // 3. Si el login fue exitoso (no lanzó excepción), guardamos la sesión localmente
        _authRepository.saveSession(token);

        return token;
    }

}
