package com.procesadoraperu.inventario.presentation.selection;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.procesadoraperu.inventario.domain.model.almacen.Almacen;
import com.procesadoraperu.inventario.domain.model.sucursal.Sucursal;
import com.procesadoraperu.inventario.domain.repository.almacen.IAlmacenRepository;
import com.procesadoraperu.inventario.domain.repository.sucursal.ISucursalRepository;
import com.procesadoraperu.inventario.domain.usecase.almacen.GetAlmacenesUseCase;
import com.procesadoraperu.inventario.domain.usecase.sucursal.GetSucursalesUseCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SelectionViewModel extends ViewModel {

    private final GetSucursalesUseCase getSucursalesUseCase;
    private final GetAlmacenesUseCase getAlmacenesUseCase;
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

    public SelectionViewModel(GetSucursalesUseCase getSucursalesUseCase,
                              GetAlmacenesUseCase getAlmacenesUseCase,
                              ISucursalRepository sucursalRepo,
                              IAlmacenRepository almacenRepo) {
        this.getSucursalesUseCase = getSucursalesUseCase;
        this.getAlmacenesUseCase = getAlmacenesUseCase;
        this.sucursalRepo = sucursalRepo;
        this.almacenRepo = almacenRepo;
    }

    // Getters para que los Activities observen los cambios
    public LiveData<List<Sucursal>> getSucursales() { return sucursales; }
    public LiveData<List<Almacen>> getAlmacenes() { return almacenes; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

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
                errorMessage.postValue("Error al cargar sucursales: " + e.getMessage());
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
                errorMessage.postValue("Error al cargar almacenes: " + e.getMessage());
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
}