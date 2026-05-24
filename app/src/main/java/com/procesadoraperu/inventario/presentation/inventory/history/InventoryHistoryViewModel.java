package com.procesadoraperu.inventario.presentation.inventory.history;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.procesadoraperu.inventario.domain.model.inventario.Inventario;
import com.procesadoraperu.inventario.domain.model.usuario.Usuario;
import com.procesadoraperu.inventario.domain.repository.almacen.IAlmacenRepository;
import com.procesadoraperu.inventario.domain.repository.sucursal.ISucursalRepository;
import com.procesadoraperu.inventario.domain.usecase.inventario.ConsultarHistorialUseCase;
import com.procesadoraperu.inventario.domain.usecase.usuario.GetActiveUserUseCase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InventoryHistoryViewModel extends ViewModel {

    private final ConsultarHistorialUseCase consultarHistorialUseCase;
    private final GetActiveUserUseCase getActiveUserUseCase;
    private final ISucursalRepository sucRepo;
    private final IAlmacenRepository almRepo;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<Inventario>> historial = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public InventoryHistoryViewModel(ConsultarHistorialUseCase consultarHistorialUseCase,
                                     GetActiveUserUseCase getActiveUserUseCase,
                                     ISucursalRepository sucRepo,
                                     IAlmacenRepository almRepo) {
        this.consultarHistorialUseCase = consultarHistorialUseCase;
        this.getActiveUserUseCase = getActiveUserUseCase;
        this.sucRepo = sucRepo;
        this.almRepo = almRepo;
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<List<Inventario>> getHistorial() { return historial; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    /** Carga el historial del día actual por defecto */
    public void cargarHistorialHoy() {
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        cargarHistorial(hoy, hoy);
    }

    /** ✔️ NUEVO MÉTODO: Carga el historial de los últimos 3 meses exactos */
    public void cargarHistorialUltimos3Meses() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        // 1. Obtener la fecha de hoy (Fecha Fin)
        String fechaFin = sdf.format(calendar.getTime());

        // 2. Restar exactamente 3 meses al calendario (Fecha Inicio)
        calendar.add(Calendar.MONTH, -3);
        String fechaInicio = sdf.format(calendar.getTime());

        // 3. Ejecutar la búsqueda
        cargarHistorial(fechaInicio, fechaFin);
    }

    public void cargarHistorial(String fechaInicio, String fechaFin) {
        isLoading.postValue(true);

        executor.execute(() -> {
            try {
                Usuario usuario = getActiveUserUseCase.execute();
                String idSucursal = sucRepo.getActiveSucursalId();
                String idAlmacen = almRepo.getActiveAlmacen() != null
                        ? almRepo.getActiveAlmacen().getIdAlmacen() : null;

                List<Inventario> resultado = consultarHistorialUseCase.execute(
                        idSucursal, idAlmacen, fechaInicio, fechaFin, usuario.getUsername()
                );
                historial.postValue(resultado);

            } catch (Exception e) {
                errorMessage.postValue("Error al cargar historial: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }
}