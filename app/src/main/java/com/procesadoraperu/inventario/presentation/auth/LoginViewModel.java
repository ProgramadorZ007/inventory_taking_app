package com.procesadoraperu.inventario.presentation.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.procesadoraperu.inventario.domain.usecase.auth.LoginUseCase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginViewModel extends ViewModel {

    private final LoginUseCase loginUseCase;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // LiveData para notificar a la vista (Activity)
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LoginViewModel(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getLoginSuccess() { return loginSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void login(String username, String password) {
        isLoading.postValue(true);

        // Ejecutamos en segundo plano para no congelar la pantalla
        executor.execute(() -> {
            try {
                loginUseCase.execute(username, password);
                loginSuccess.postValue(true);
            } catch (Exception e) {
                errorMessage.postValue(e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }
}