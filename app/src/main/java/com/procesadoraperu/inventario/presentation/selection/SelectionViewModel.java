package com.procesadoraperu.inventario.presentation.selection;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.procesadoraperu.inventario.domain.model.DownloadResult;
import com.procesadoraperu.inventario.domain.model.almacen.Almacen;
import com.procesadoraperu.inventario.domain.model.sucursal.Sucursal;
import com.procesadoraperu.inventario.domain.repository.almacen.IAlmacenRepository;
import com.procesadoraperu.inventario.domain.repository.sucursal.ISucursalRepository;
import com.procesadoraperu.inventario.domain.usecase.almacen.GetAlmacenesUseCase;
import com.procesadoraperu.inventario.domain.usecase.producto.DownloadCatalogUseCase;
import com.procesadoraperu.inventario.domain.usecase.sucursal.GetSucursalesUseCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SelectionViewModel extends ViewModel {

    private final GetSucursalesUseCase getSucursalesUseCase;
    private final GetAlmacenesUseCase getAlmacenesUseCase;
    private final DownloadCatalogUseCase downloadCatalogUseCase;
    private final ISucursalRepository sucursalRepo;
    private final IAlmacenRepository almacenRepo;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Listas Originales (Para no volver a consultar a la BD al borrar el texto del buscador)
    private List<Sucursal> listaOriginalSucursales = new ArrayList<>();
    private List<Almacen> listaOriginalAlmacenes = new ArrayList<>();

    // LiveData que escucha la interfaz (Activity)
    private final MutableLiveData<List<Sucursal>> sucursales = new MutableLiveData<>();
    private final MutableLiveData<List<Almacen>> almacenes = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Estado de descarga del catálogo
    private final MutableLiveData<Boolean> isDownloading = new MutableLiveData<>(false);
    private final MutableLiveData<DownloadResult> downloadResult = new MutableLiveData<>();

    public SelectionViewModel(GetSucursalesUseCase getSucursalesUseCase,
                              GetAlmacenesUseCase getAlmacenesUseCase,
                              DownloadCatalogUseCase downloadCatalogUseCase,
                              ISucursalRepository sucursalRepo,
                              IAlmacenRepository almacenRepo) {
        this.getSucursalesUseCase = getSucursalesUseCase;
        this.getAlmacenesUseCase = getAlmacenesUseCase;
        this.downloadCatalogUseCase = downloadCatalogUseCase;
        this.sucursalRepo = sucursalRepo;
        this.almacenRepo = almacenRepo;
    }

    // Getters para que los Activities observen los cambios
    public LiveData<List<Sucursal>> getSucursales() { return sucursales; }
    public LiveData<List<Almacen>> getAlmacenes() { return almacenes; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsDownloading() { return isDownloading; }
    public LiveData<DownloadResult> getDownloadResult() { return downloadResult; }

    // ==========================================
    // LÓGICA SUCURSAL
    // ==========================================
    public void cargarSucursales() {
        executor.execute(() -> {
            try {
                // False = prioriza la base de datos local SQLite
                listaOriginalSucursales = getSucursalesUseCase.execute(false);
                sucursales.postValue(listaOriginalSucursales);
            } catch (Exception e) {
                errorMessage.postValue(obtenerMensajeAmigable("cargar las sucursales", e));
            }
        });
    }

    public void filtrarSucursales(String query) {
        if (query == null || query.trim().isEmpty()) {
            sucursales.postValue(listaOriginalSucursales);
            return;
        }
        List<Sucursal> filtrados = new ArrayList<>();
        for (Sucursal s : listaOriginalSucursales) {
            if (s.getDescripcion().toLowerCase().contains(query.toLowerCase())) {
                filtrados.add(s);
            }
        }
        sucursales.postValue(filtrados);
    }

    public void guardarSucursalSeleccionada(Sucursal sucursal) {
        // Guardamos el ID en SharedPreferences a través del repositorio
        sucursalRepo.saveActiveSucursalId(sucursal.getIdSucursal());
    }

    // ==========================================
    // LÓGICA ALMACÉN
    // ==========================================
    public void cargarAlmacenes(String idSucursal) {
        executor.execute(() -> {
            try {
                // False = prioriza la base de datos local SQLite
                listaOriginalAlmacenes = getAlmacenesUseCase.execute(idSucursal, false);
                almacenes.postValue(listaOriginalAlmacenes);
            } catch (Exception e) {
                errorMessage.postValue(obtenerMensajeAmigable("cargar los almacenes", e));
            }
        });
    }

    public void filtrarAlmacenes(String query) {
        if (query == null || query.trim().isEmpty()) {
            almacenes.postValue(listaOriginalAlmacenes);
            return;
        }
        List<Almacen> filtrados = new ArrayList<>();
        for (Almacen a : listaOriginalAlmacenes) {
            if (a.getDescripcion().toLowerCase().contains(query.toLowerCase())) {
                filtrados.add(a);
            }
        }
        almacenes.postValue(filtrados);
    }

    public void guardarAlmacenSeleccionado(Almacen almacen) {
        // Guardamos el objeto completo en SharedPreferences a través del repositorio
        almacenRepo.saveActiveAlmacen(almacen);
    }

    // ==========================================
    // LÓGICA DESCARGA DE CATÁLOGO
    // ==========================================
    public void downloadCatalogAndNavigate(String idSucursal, String idAlmacen) {
        isDownloading.postValue(true);
        executor.execute(() -> {
            try {
                int count = downloadCatalogUseCase.execute(idSucursal, idAlmacen);
                if (count > 0) {
                    downloadResult.postValue(DownloadResult.success(count));
                } else {
                    downloadResult.postValue(DownloadResult.empty());
                }
            } catch (Exception e) {
                downloadResult.postValue(DownloadResult.error(obtenerMensajeAmigable("descargar el catálogo", e)));
            } finally {
                isDownloading.postValue(false);
            }
        });
    }

    // ===================================================================
    // Traducción de errores técnicos a mensajes amigables para el usuario
    // ===================================================================

    private String obtenerMensajeAmigable(String accion, Exception e) {
        if (e == null) return "No se pudo " + accion;

        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        if (esErrorDeConexion(e, msg)) {
            return "Sin conexión a internet. Verifica tu conexión e inténtalo de nuevo.";
        }

        if (msg.contains("timeout") || msg.contains("timed out")) {
            return "La conexión tardó demasiado. Verifica tu internet e inténtalo de nuevo.";
        }

        if (msg.contains("token") || msg.contains("unauthorized") || msg.contains("401")) {
            return "Tu sesión ha expirado. Cierra sesión e inicia nuevamente.";
        }

        if (msg.contains("server") || msg.contains("500") || msg.contains("internal")) {
            return "El servidor no está disponible en este momento. Inténtalo más tarde.";
        }

        return "No se pudo " + accion + ". Inténtalo de nuevo.";
    }

    private boolean esErrorDeConexion(Exception e, String msg) {
        return e instanceof java.net.UnknownHostException
                || e instanceof java.net.ConnectException
                || e instanceof java.net.NoRouteToHostException
                || msg.contains("unable to resolve host")
                || msg.contains("failed to connect")
                || msg.contains("no address associated")
                || msg.contains("network is unreachable")
                || msg.contains("connection refused")
                || msg.contains("no internet");
    }
}