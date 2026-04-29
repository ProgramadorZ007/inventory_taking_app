package com.procesadoraperu.inventario.presentation.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.procesadoraperu.inventario.domain.model.sucursal.Sucursal;
import com.procesadoraperu.inventario.domain.usecase.auth.LogoutUseCase;
import com.procesadoraperu.inventario.domain.usecase.sucursal.GetSucursalesUseCase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeViewModel extends ViewModel {

    private final GetSucursalesUseCase getSucursalesUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<Sucursal>> sucursalesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> logoutSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public HomeViewModel(GetSucursalesUseCase getSucursalesUseCase, LogoutUseCase logoutUseCase) {
        this.getSucursalesUseCase = getSucursalesUseCase;
        this.logoutUseCase = logoutUseCase;
    }

    public LiveData<List<Sucursal>> getSucursales() { return sucursalesLiveData; }
    public LiveData<Boolean> getLogoutSuccess() { return logoutSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void cargarSucursales() {
        executor.execute(() -> {
            try {
                // False = prioriza la base de datos local SQLite si no fuerza la recarga
                List<Sucursal> lista = getSucursalesUseCase.execute(false);
                sucursalesLiveData.postValue(lista);
            } catch (Exception e) {
                errorMessage.postValue("Error al cargar sucursales: " + e.getMessage());
            }
        });
    }

    public void cerrarSesion() {
        executor.execute(() -> {
            logoutUseCase.execute();
            logoutSuccess.postValue(true);
        });
    }
}