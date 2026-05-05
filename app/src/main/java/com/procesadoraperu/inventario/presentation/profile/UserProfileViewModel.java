package com.procesadoraperu.inventario.presentation.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.procesadoraperu.inventario.domain.model.almacen.Almacen;
import com.procesadoraperu.inventario.domain.model.usuario.Usuario;
import com.procesadoraperu.inventario.domain.repository.almacen.IAlmacenRepository;
import com.procesadoraperu.inventario.domain.repository.sucursal.ISucursalRepository;
import com.procesadoraperu.inventario.domain.usecase.usuario.GetActiveUserUseCase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserProfileViewModel extends ViewModel {

    private final GetActiveUserUseCase getActiveUserUseCase;
    private final ISucursalRepository sucRepo;
    private final IAlmacenRepository almRepo;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<ProfileData> profileData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public static class ProfileData {
        public final String username;
        public final String nombres;
        public final String idCodigoGeneral;
        public final String sucursal;
        public final String almacen;
        public final String idAlmacen;

        public ProfileData(String username, String nombres, String idCodigoGeneral,
                           String sucursal, String almacen, String idAlmacen) {
            this.username = username;
            this.nombres = nombres;
            this.idCodigoGeneral = idCodigoGeneral;
            this.sucursal = sucursal;
            this.almacen = almacen;
            this.idAlmacen = idAlmacen;
        }
    }

    public UserProfileViewModel(GetActiveUserUseCase getActiveUserUseCase,
                                ISucursalRepository sucRepo,
                                IAlmacenRepository almRepo) {
        this.getActiveUserUseCase = getActiveUserUseCase;
        this.sucRepo = sucRepo;
        this.almRepo = almRepo;
    }

    public LiveData<ProfileData> getProfileData() { return profileData; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void cargarPerfil() {
        executor.execute(() -> {
            try {
                Usuario usuario = getActiveUserUseCase.execute();
                String idSucursal = sucRepo.getActiveSucursalId();
                Almacen almacen  = almRepo.getActiveAlmacen();

                String sucursalDesc = (idSucursal != null)
                        ? "Sucursal " + idSucursal : "No asignada";
                String almacenDesc  = (almacen != null)
                        ? almacen.getDescripcion() : "No asignado";
                String idAlmacenStr = (almacen != null) ? almacen.getIdAlmacen() : "—";

                profileData.postValue(new ProfileData(
                        usuario.getUsername(),
                        usuario.getNombres(),
                        (usuario.getIdCodigoGeneral() != null && !usuario.getIdCodigoGeneral().isEmpty())
                                ? usuario.getIdCodigoGeneral() : "—",
                        sucursalDesc,
                        almacenDesc,
                        idAlmacenStr
                ));
            } catch (Exception e) {
                errorMessage.postValue("Error al cargar perfil: " + e.getMessage());
            }
        });
    }
}