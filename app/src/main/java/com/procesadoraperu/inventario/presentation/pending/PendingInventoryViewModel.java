package com.procesadoraperu.inventario.presentation.inventory.pending;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.procesadoraperu.inventario.domain.model.inventario.Inventario;
import com.procesadoraperu.inventario.domain.model.usuario.Usuario;
import com.procesadoraperu.inventario.domain.usecase.inventario.GetInventariosPendientesUseCase;
import com.procesadoraperu.inventario.domain.usecase.inventario.SincronizarPendientesUseCase;
import com.procesadoraperu.inventario.domain.usecase.usuario.GetActiveUserUseCase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PendingInventoryViewModel extends ViewModel {

    private final GetInventariosPendientesUseCase getPendientesUseCase;
    private final SincronizarPendientesUseCase sincronizarUseCase;
    private final GetActiveUserUseCase getActiveUserUseCase;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Boolean> isLoading       = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSincronizando = new MutableLiveData<>(false);
    private final MutableLiveData<List<Inventario>> pendientes = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage     = new MutableLiveData<>();
    private final MutableLiveData<String> syncResultMessage = new MutableLiveData<>();

    public PendingInventoryViewModel(GetInventariosPendientesUseCase getPendientesUseCase,
                                     SincronizarPendientesUseCase sincronizarUseCase,
                                     GetActiveUserUseCase getActiveUserUseCase) {
        this.getPendientesUseCase = getPendientesUseCase;
        this.sincronizarUseCase = sincronizarUseCase;
        this.getActiveUserUseCase = getActiveUserUseCase;
    }

    public LiveData<Boolean> getIsLoading()        { return isLoading; }
    public LiveData<Boolean> getIsSincronizando()  { return isSincronizando; }
    public LiveData<List<Inventario>> getPendientes() { return pendientes; }
    public LiveData<String> getErrorMessage()      { return errorMessage; }
    public LiveData<String> getSyncResultMessage() { return syncResultMessage; }

    public void cargarPendientes() {
        isLoading.postValue(true);
        executor.execute(() -> {
            try {
                Usuario usuario = getActiveUserUseCase.execute();
                List<Inventario> lista = getPendientesUseCase.execute(usuario.getUsername());
                pendientes.postValue(lista);
            } catch (Exception e) {
                errorMessage.postValue("Error al cargar pendientes: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void sincronizarTodos() {
        isSincronizando.postValue(true);
        executor.execute(() -> {
            try {
                Usuario usuario = getActiveUserUseCase.execute();
                int fallidos = sincronizarUseCase.execute(usuario.getUsername());

                if (fallidos == 0) {
                    syncResultMessage.postValue("✅ Todos los registros se sincronizaron exitosamente.");
                } else {
                    syncResultMessage.postValue("⚠️ " + fallidos + " registro(s) no pudieron sincronizarse. Verifica tu conexión.");
                }

                // Recargar la lista
                List<Inventario> lista = getPendientesUseCase.execute(usuario.getUsername());
                pendientes.postValue(lista);

            } catch (Exception e) {
                errorMessage.postValue("Error durante la sincronización: " + e.getMessage());
            } finally {
                isSincronizando.postValue(false);
            }
        });
    }
}