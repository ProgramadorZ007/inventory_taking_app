package com.procesadoraperu.inventario.domain.usecase.auth;

import com.procesadoraperu.inventario.domain.repository.auth.IAuthRepository;

public class LogoutUseCase {

    private final IAuthRepository authRepository;

    public LogoutUseCase(IAuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public void execute() {
        // Avisa al servidor que invalide el token y borra los datos locales
        authRepository.logout();
    }

}
