package com.procesadoraperu.inventario.presentation.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.procesadoraperu.inventario.domain.model.almacen.Almacen;
import com.procesadoraperu.inventario.domain.model.sucursal.Sucursal;
import com.procesadoraperu.inventario.domain.model.usuario.Usuario;
import com.procesadoraperu.inventario.domain.repository.almacen.IAlmacenRepository;
import com.procesadoraperu.inventario.domain.repository.auth.IAuthRepository;
import com.procesadoraperu.inventario.domain.repository.sucursal.ISucursalRepository;
import com.procesadoraperu.inventario.domain.usecase.auth.LogoutUseCase;
import com.procesadoraperu.inventario.domain.usecase.producto.ClearCatalogUseCase;
import com.procesadoraperu.inventario.domain.usecase.producto.GetCatalogCountUseCase;
import com.procesadoraperu.inventario.domain.usecase.usuario.GetActiveUserUseCase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeViewModel extends ViewModel {

    private final LogoutUseCase logoutUseCase;
    private final IAuthRepository authRepo;
    private final ISucursalRepository sucRepo;
    private final IAlmacenRepository almRepo;
    private final GetActiveUserUseCase getActiveUserUseCase;
    private final GetCatalogCountUseCase getCatalogCountUseCase;
    private final ClearCatalogUseCase clearCatalogUseCase;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<HeaderData> headerData   = new MutableLiveData<>();
    private final MutableLiveData<Boolean> logoutSuccess   = new MutableLiveData<>();
    private final MutableLiveData<Integer> catalogCount    = new MutableLiveData<>();
    private final MutableLiveData<Boolean> clearSuccess    = new MutableLiveData<>();

    // ── Data class para la cabecera ───────────────────────────────────────────
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

    public HomeViewModel(LogoutUseCase logoutUseCase,
                         IAuthRepository authRepo,
                         ISucursalRepository sucRepo,
                         IAlmacenRepository almRepo,
                         GetActiveUserUseCase getActiveUserUseCase,
                         GetCatalogCountUseCase getCatalogCountUseCase,
                         ClearCatalogUseCase clearCatalogUseCase) {
        this.logoutUseCase          = logoutUseCase;
        this.authRepo               = authRepo;
        this.sucRepo                = sucRepo;
        this.almRepo                = almRepo;
        this.getActiveUserUseCase   = getActiveUserUseCase;
        this.getCatalogCountUseCase = getCatalogCountUseCase;
        this.clearCatalogUseCase    = clearCatalogUseCase;
    }

    public LiveData<HeaderData> getHeaderData()   { return headerData; }
    public LiveData<Boolean> getLogoutSuccess()   { return logoutSuccess; }
    public LiveData<Integer> getCatalogCount()    { return catalogCount; }
    public LiveData<Boolean> getClearSuccess()    { return clearSuccess; }

    /**
     * Carga los datos del encabezado: usuario + sucursal activa + almacén activo.
     * Intenta obtener la descripción real de la sucursal desde SQLite.
     */
    public void cargarDatosCabecera() {
        executor.execute(() -> {
            try {
                // 1. Usuario
                Usuario usuario = getActiveUserUseCase.execute();

                // 2. Almacén activo (tiene descripción guardada en prefs)
                Almacen almacen = almRepo.getActiveAlmacen();
                String nombreAlmacen = (almacen != null)
                        ? almacen.getDescripcion() : "Sin almacén";

                // 3. Sucursal activa: intentamos obtener la descripción desde SQLite
                String idSucursal = sucRepo.getActiveSucursalId();
                String nombreSucursal = resolverNombreSucursal(idSucursal, almacen);

                headerData.postValue(new HeaderData(
                        usuario.getNombres(),
                        nombreSucursal,
                        nombreAlmacen
                ));

            } catch (Exception e) {
                // En caso de error, mostramos datos mínimos sin crashear
                headerData.postValue(new HeaderData("Operario", "—", "—"));
            }
        });
    }

    /**
     * Resuelve el nombre descriptivo de la sucursal.
     * Prioriza la descripción del almacén (que tiene el idSucursal) o hace lookup en SQLite.
     */
    private String resolverNombreSucursal(String idSucursal, Almacen almacen) {
        if (idSucursal == null) return "Sin sucursal";

        try {
            // Busca en la lista local guardada de sucursales
            List<com.procesadoraperu.inventario.domain.model.sucursal.Sucursal> locales =
                    sucRepo.getSucursalesLocal();
            if (locales != null) {
                for (Sucursal s : locales) {
                    if (idSucursal.equals(s.getIdSucursal())) {
                        return s.getDescripcion();
                    }
                }
            }
        } catch (Exception ignored) {}

        // Fallback: mostrar el ID de la sucursal si no hay descripción
        return "Sucursal " + idSucursal;
    }

    public void loadCatalogCount() {
        executor.execute(() -> {
            int count = getCatalogCountUseCase.execute();
            catalogCount.postValue(count);
        });
    }

    public void clearCatalog() {
        executor.execute(() -> {
            try {
                clearCatalogUseCase.execute();
                clearSuccess.postValue(true);
            } catch (Exception e) {
                clearSuccess.postValue(false);
            }
            loadCatalogCount();
        });
    }

    public void cerrarSesion() {
        executor.execute(() -> {
            logoutUseCase.execute();
            logoutSuccess.postValue(true);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}