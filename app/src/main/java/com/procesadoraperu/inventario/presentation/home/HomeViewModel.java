package com.procesadoraperu.inventario.presentation.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.procesadoraperu.inventario.domain.model.almacen.Almacen;
import com.procesadoraperu.inventario.domain.repository.almacen.IAlmacenRepository;
import com.procesadoraperu.inventario.domain.repository.auth.IAuthRepository;
import com.procesadoraperu.inventario.domain.repository.sucursal.ISucursalRepository;
import com.procesadoraperu.inventario.domain.usecase.auth.LogoutUseCase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeViewModel extends ViewModel {

    private final LogoutUseCase logoutUseCase;
    private final IAuthRepository authRepo;
    private final ISucursalRepository sucRepo;
    private final IAlmacenRepository almRepo;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<String> headerInfo = new MutableLiveData<>();
    private final MutableLiveData<Boolean> logoutSuccess = new MutableLiveData<>();

    // AHORA SÍ RECIBE LOS 4 PARÁMETROS DEL FACTORY
    public HomeViewModel(LogoutUseCase logoutUseCase, IAuthRepository authRepo,
                         ISucursalRepository sucRepo, IAlmacenRepository almRepo) {
        this.logoutUseCase = logoutUseCase;
        this.authRepo = authRepo;
        this.sucRepo = sucRepo;
        this.almRepo = almRepo;
    }

    public LiveData<String> getHeaderInfo() { return headerInfo; }
    public LiveData<Boolean> getLogoutSuccess() { return logoutSuccess; }

    // Función para armar el texto "Sucursal 1 | Almacén Materia Prima"
    public void cargarDatosCabecera() {
        executor.execute(() -> {
            String idSucursal = sucRepo.getActiveSucursalId();
            Almacen almacen = almRepo.getActiveAlmacen();

            String nombreSucursal = (idSucursal != null) ? "Sucursal " + idSucursal : "Sin sucursal";
            String nombreAlmacen = (almacen != null) ? almacen.getDescripcion() : "Sin almacén";

            headerInfo.postValue(nombreSucursal + " | " + nombreAlmacen);
        });
    }

    public void cerrarSesion() {
        executor.execute(() -> {
            logoutUseCase.execute();
            logoutSuccess.postValue(true);
        });
    }
}