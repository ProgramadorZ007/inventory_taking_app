package com.procesadoraperu.inventario.presentation.inventory.take;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.procesadoraperu.inventario.domain.model.almacen.Almacen;
import com.procesadoraperu.inventario.domain.model.inventario.Inventario;
import com.procesadoraperu.inventario.domain.model.producto.Producto;
import com.procesadoraperu.inventario.domain.model.usuario.Usuario;
import com.procesadoraperu.inventario.domain.repository.almacen.IAlmacenRepository;
import com.procesadoraperu.inventario.domain.repository.sucursal.ISucursalRepository;
import com.procesadoraperu.inventario.domain.usecase.inventario.RegistrarInventarioUseCase;
import com.procesadoraperu.inventario.domain.usecase.producto.ConsultarStockProductoUseCase;
import com.procesadoraperu.inventario.domain.usecase.usuario.GetActiveUserUseCase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TakeInventoryViewModel extends ViewModel {

    private final ConsultarStockProductoUseCase consultarStockUseCase;
    private final RegistrarInventarioUseCase registrarInventarioUseCase;
    private final GetActiveUserUseCase getActiveUserUseCase;
    private final ISucursalRepository sucRepo;
    private final IAlmacenRepository almRepo;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Estado de la pantalla
    private final MutableLiveData<Boolean> isLoadingProducto = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isRegistrando    = new MutableLiveData<>(false);
    private final MutableLiveData<Producto> productoEncontrado = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage       = new MutableLiveData<>();
    private final MutableLiveData<RegistroResult> registroResult = new MutableLiveData<>();

    public enum RegistroResult { SINCRONIZADO, GUARDADO_LOCAL, ERROR }

    public TakeInventoryViewModel(ConsultarStockProductoUseCase consultarStockUseCase,
                                  RegistrarInventarioUseCase registrarInventarioUseCase,
                                  GetActiveUserUseCase getActiveUserUseCase,
                                  ISucursalRepository sucRepo,
                                  IAlmacenRepository almRepo) {
        this.consultarStockUseCase = consultarStockUseCase;
        this.registrarInventarioUseCase = registrarInventarioUseCase;
        this.getActiveUserUseCase = getActiveUserUseCase;
        this.sucRepo = sucRepo;
        this.almRepo = almRepo;
    }

    public LiveData<Boolean> getIsLoadingProducto() { return isLoadingProducto; }
    public LiveData<Boolean> getIsRegistrando()     { return isRegistrando; }
    public LiveData<Producto> getProductoEncontrado() { return productoEncontrado; }
    public LiveData<String> getErrorMessage()        { return errorMessage; }
    public LiveData<RegistroResult> getRegistroResult() { return registroResult; }

    /** Busca el producto por su código (barcode o manual) */
    public void buscarProducto(String idProducto) {
        if (idProducto == null || idProducto.trim().isEmpty()) {
            errorMessage.postValue("Ingresa un código de producto válido");
            return;
        }

        isLoadingProducto.postValue(true);
        productoEncontrado.postValue(null);

        executor.execute(() -> {
            try {
                String idSucursal = sucRepo.getActiveSucursalId();
                Almacen almacen = almRepo.getActiveAlmacen();

                if (idSucursal == null || almacen == null) {
                    errorMessage.postValue("Debes seleccionar una sucursal y almacén primero");
                    return;
                }

                Producto producto = consultarStockUseCase.execute(
                        idSucursal, almacen.getIdAlmacen(), idProducto.trim()
                );
                productoEncontrado.postValue(producto);
            } catch (Exception e) {
                errorMessage.postValue("Producto no encontrado: " + e.getMessage());
            } finally {
                isLoadingProducto.postValue(false);
            }
        });
    }

    /** Registra el inventario con la cantidad ingresada por el operario */
    public void registrarInventario(Producto producto, double cantidadContada) {
        if (producto == null) {
            errorMessage.postValue("Primero busca un producto");
            return;
        }
        if (cantidadContada < 0) {
            errorMessage.postValue("La cantidad no puede ser negativa");
            return;
        }

        isRegistrando.postValue(true);

        executor.execute(() -> {
            try {
                Usuario usuario = getActiveUserUseCase.execute();
                String idSucursal = sucRepo.getActiveSucursalId();
                Almacen almacen = almRepo.getActiveAlmacen();
                String fechaActual = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date());

                // Construimos el objeto Inventario
                Inventario inventario = new Inventario();
                inventario.setIdEmpresa("001");
                inventario.setIdSucursal(idSucursal);
                inventario.setSucursal("Sucursal " + idSucursal);
                inventario.setIdAlmacen(almacen.getIdAlmacen());
                inventario.setAlmacen(almacen.getDescripcion());
                inventario.setIdProducto(producto.getIdProducto());
                inventario.setProducto(producto.getDescripcion());
                inventario.setUnidadMedida(producto.getIdMedida());
                inventario.setStock(producto.getStock().doubleValue());
                inventario.setCantidad(cantidadContada);
                inventario.setUsuarioCreacion(usuario.getUsername());
                inventario.setFechaCreacion(fechaActual);
                inventario.setFechaRegistroLocal(fechaActual);

                // El UseCase maneja envío + fallback local
                registrarInventarioUseCase.execute(inventario);

                // Determinar resultado según el estado que quedó
                RegistroResult result = "SINCRONIZADO".equals(inventario.getEstadoSincronizacion())
                        ? RegistroResult.SINCRONIZADO
                        : RegistroResult.GUARDADO_LOCAL;
                registroResult.postValue(result);

            } catch (Exception e) {
                errorMessage.postValue("Error inesperado: " + e.getMessage());
                registroResult.postValue(RegistroResult.ERROR);
            } finally {
                isRegistrando.postValue(false);
            }
        });
    }

    /** Limpia el estado para registrar otro producto */
    public void limpiarEstado() {
        productoEncontrado.postValue(null);
        errorMessage.postValue(null);
        registroResult.postValue(null);
    }
}