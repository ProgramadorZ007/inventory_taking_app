package com.procesadoraperu.inventario.presentation;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.procesadoraperu.inventario.core.location.AuditClientInfoProvider;
import com.procesadoraperu.inventario.core.network.ApiClient;
import com.procesadoraperu.inventario.data.local.database.InventarioDatabase;
import com.procesadoraperu.inventario.data.remote.api.AlmacenApi;
import com.procesadoraperu.inventario.data.remote.api.AuthApi;
import com.procesadoraperu.inventario.data.remote.api.InventarioApi;
import com.procesadoraperu.inventario.data.remote.api.ProductoApi;
import com.procesadoraperu.inventario.data.remote.api.SucursalApi;
import com.procesadoraperu.inventario.data.repository.AlmacenRepositoryImpl;
import com.procesadoraperu.inventario.data.repository.AuthRepositoryImpl;
import com.procesadoraperu.inventario.data.repository.InventarioRepositoryImpl;
import com.procesadoraperu.inventario.data.repository.LogRepositoryImpl;
import com.procesadoraperu.inventario.data.repository.ProductoRepositoryImpl;
import com.procesadoraperu.inventario.data.repository.SucursalRepositoryImpl;
import com.procesadoraperu.inventario.data.repository.UsuarioRepositoryImpl;
import com.procesadoraperu.inventario.domain.provider.IAuditClientInfoProvider;
import com.procesadoraperu.inventario.domain.repository.almacen.IAlmacenRepository;
import com.procesadoraperu.inventario.domain.repository.auth.IAuthRepository;
import com.procesadoraperu.inventario.domain.repository.inventario.IInventarioRepository;
import com.procesadoraperu.inventario.domain.repository.log.ILogRepository;
import com.procesadoraperu.inventario.domain.repository.producto.IProductoRepository;
import com.procesadoraperu.inventario.domain.repository.sucursal.ISucursalRepository;
import com.procesadoraperu.inventario.domain.repository.usuario.IUsuarioRepository;
import com.procesadoraperu.inventario.domain.usecase.almacen.GetAlmacenesUseCase;
import com.procesadoraperu.inventario.domain.usecase.auth.LoginUseCase;
import com.procesadoraperu.inventario.domain.usecase.auth.LogoutUseCase;
import com.procesadoraperu.inventario.domain.usecase.inventario.ConsultarHistorialUseCase;
import com.procesadoraperu.inventario.domain.usecase.inventario.GetInventariosPendientesUseCase;
import com.procesadoraperu.inventario.domain.usecase.inventario.RegistrarInventarioUseCase;
import com.procesadoraperu.inventario.domain.usecase.inventario.SincronizarPendientesUseCase;
import com.procesadoraperu.inventario.domain.usecase.producto.ConsultarStockProductoUseCase;
import com.procesadoraperu.inventario.domain.usecase.sucursal.GetSucursalesUseCase;
import com.procesadoraperu.inventario.domain.usecase.usuario.GetActiveUserUseCase;
import com.procesadoraperu.inventario.presentation.auth.LoginViewModel;
import com.procesadoraperu.inventario.presentation.home.HomeViewModel;
import com.procesadoraperu.inventario.presentation.inventory.history.InventoryHistoryViewModel;
import com.procesadoraperu.inventario.presentation.inventory.pending.PendingInventoryViewModel;
import com.procesadoraperu.inventario.presentation.inventory.take.TakeInventoryViewModel;
import com.procesadoraperu.inventario.presentation.profile.UserProfileViewModel;
import com.procesadoraperu.inventario.presentation.selection.SelectionViewModel;

import retrofit2.Retrofit;

/**
 * Fábrica central de ViewModels.
 * Implementa el patrón de Inyección de Dependencias manual.
 * Todas las dependencias se construyen aquí y se inyectan en los ViewModels.
 */
public class ViewModelFactory implements ViewModelProvider.Factory {

    private final Context context;

    public ViewModelFactory(Context context) {
        // Usamos ApplicationContext para evitar memory leaks
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {

        // ── Infraestructura ──────────────────────────────────────────────────
        InventarioDatabase db  = InventarioDatabase.getInstance(context);
        Retrofit retrofit      = ApiClient.getClient(context);

        // SharedPreferences para estado de sesión (sucursal/almacén activos)
        SharedPreferences appPrefs = context.getSharedPreferences(
                "app_prefs", Context.MODE_PRIVATE);

        // ── Repositorios ─────────────────────────────────────────────────────
        IAuthRepository authRepo = new AuthRepositoryImpl(
                retrofit.create(AuthApi.class),
                db.usuarioDao(),
                context
        );

        ISucursalRepository sucRepo = new SucursalRepositoryImpl(
                retrofit.create(SucursalApi.class),
                db.sucursalDao(),
                appPrefs
        );

        IAlmacenRepository almRepo = new AlmacenRepositoryImpl(
                retrofit.create(AlmacenApi.class),
                db.almacenDao(),
                appPrefs
        );

        IInventarioRepository invRepo = new InventarioRepositoryImpl(
                retrofit.create(InventarioApi.class),
                db.inventarioDao()
        );

        IProductoRepository prodRepo = new ProductoRepositoryImpl(
                retrofit.create(ProductoApi.class),
                db.productoDao()
        );

        ILogRepository logRepo = new LogRepositoryImpl(db.logDao());

        IUsuarioRepository usuarioRepo = new UsuarioRepositoryImpl(db.usuarioDao());

        // ── Provider de auditoría (GPS + dispositivo) ────────────────────────
        IAuditClientInfoProvider auditProvider = new AuditClientInfoProvider(context);

        // ── Casos de Uso ─────────────────────────────────────────────────────
        LoginUseCase loginUseCase                           = new LoginUseCase(authRepo);
        LogoutUseCase logoutUseCase                         = new LogoutUseCase(authRepo);
        GetSucursalesUseCase getSucursalesUseCase           = new GetSucursalesUseCase(sucRepo);
        GetAlmacenesUseCase getAlmacenesUseCase             = new GetAlmacenesUseCase(almRepo);
        GetActiveUserUseCase getActiveUserUseCase           = new GetActiveUserUseCase(usuarioRepo);
        ConsultarStockProductoUseCase consultarStockUseCase = new ConsultarStockProductoUseCase(prodRepo);
        RegistrarInventarioUseCase registrarInvUseCase      = new RegistrarInventarioUseCase(invRepo, logRepo, auditProvider);
        ConsultarHistorialUseCase consultarHistorialUseCase = new ConsultarHistorialUseCase(invRepo);
        GetInventariosPendientesUseCase getPendientesUC     = new GetInventariosPendientesUseCase(invRepo);
        SincronizarPendientesUseCase sincronizarUseCase     = new SincronizarPendientesUseCase(invRepo, logRepo);

        // ── Construcción del ViewModel solicitado ────────────────────────────
        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            return (T) new LoginViewModel(loginUseCase);

        } else if (modelClass.isAssignableFrom(SelectionViewModel.class)) {
            return (T) new SelectionViewModel(
                    getSucursalesUseCase, getAlmacenesUseCase, sucRepo, almRepo);

        } else if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            return (T) new HomeViewModel(
                    logoutUseCase, authRepo, sucRepo, almRepo, getActiveUserUseCase);

        } else if (modelClass.isAssignableFrom(TakeInventoryViewModel.class)) {
            return (T) new TakeInventoryViewModel(
                    consultarStockUseCase, registrarInvUseCase, getActiveUserUseCase, sucRepo, almRepo);

        } else if (modelClass.isAssignableFrom(InventoryHistoryViewModel.class)) {
            return (T) new InventoryHistoryViewModel(
                    consultarHistorialUseCase, getActiveUserUseCase, sucRepo, almRepo);

        } else if (modelClass.isAssignableFrom(PendingInventoryViewModel.class)) {
            return (T) new PendingInventoryViewModel(
                    getPendientesUC, sincronizarUseCase, getActiveUserUseCase);

        } else if (modelClass.isAssignableFrom(UserProfileViewModel.class)) {
            return (T) new UserProfileViewModel(getActiveUserUseCase, sucRepo, almRepo);
        }

        throw new IllegalArgumentException(
                "ViewModel no registrado en ViewModelFactory: " + modelClass.getName());
    }
}