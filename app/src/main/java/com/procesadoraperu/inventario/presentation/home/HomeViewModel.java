package com.procesadoraperu.inventario.presentation.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.procesadoraperu.inventario.domain.model.almacen.Almacen;
import com.procesadoraperu.inventario.domain.model.usuario.Usuario;
import com.procesadoraperu.inventario.domain.repository.almacen.IAlmacenRepository;
import com.procesadoraperu.inventario.domain.repository.auth.IAuthRepository;
import com.procesadoraperu.inventario.domain.repository.sucursal.ISucursalRepository;
import com.procesadoraperu.inventario.domain.usecase.auth.LogoutUseCase;
import com.procesadoraperu.inventario.domain.usecase.usuario.GetActiveUserUseCase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeViewModel extends ViewModel {

    private final LogoutUseCase logoutUseCase;
    private final IAuthRepository authRepo;
    private final ISucursalRepository sucRepo;
    private final IAlmacenRepository almRepo;
    private final GetActiveUserUseCase getActiveUserUseCase;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<HeaderData> headerData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> logoutSuccess = new MutableLiveData<>();

    public static class HeaderData {
        public final String nombreUsuario;
        public final String sucursal;
        public final String almacen;

        public HeaderData(String nombreUsuario, String sucursal, String almacen) {
            this.nombreUsuario = nombreUsuario;
            this.sucursal = sucursal;
            this.almacen = almacen;
        }
    }

    public HomeViewModel(LogoutUseCase logoutUseCase, IAuthRepository authRepo,
                         ISucursalRepository sucRepo, IAlmacenRepository almRepo,
                         GetActiveUserUseCase getActiveUserUseCase) {
        this.logoutUseCase = logoutUseCase;
        this.authRepo = authRepo;
        this.sucRepo = sucRepo;
        this.almRepo = almRepo;
        this.getActiveUserUseCase = getActiveUserUseCase;
    }

    public LiveData<HeaderData> getHeaderData() { return headerData; }
    public LiveData<Boolean> getLogoutSuccess() { return logoutSuccess; }

    public void cargarDatosCabecera() {
        executor.execute(() -> {
            try {
                Usuario usuario = getActiveUserUseCase.execute();
                String idSucursal = sucRepo.getActiveSucursalId();
                Almacen almacen = almRepo.getActiveAlmacen();

                String nombreSucursal = (idSucursal != null) ? "Sucursal " + idSucursal : "Sin sucursal";
                String nombreAlmacen = (almacen != null) ? almacen.getDescripcion() : "Sin almacén";
                String nombreUsuario = usuario.getNombres();

                headerData.postValue(new HeaderData(nombreUsuario, nombreSucursal, nombreAlmacen));
            } catch (Exception e) {
                headerData.postValue(new HeaderData("Operario", "—", "—"));
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