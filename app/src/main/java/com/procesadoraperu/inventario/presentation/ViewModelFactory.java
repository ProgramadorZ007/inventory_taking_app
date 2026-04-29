package com.procesadoraperu.inventario.presentation;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.procesadoraperu.inventario.core.network.ApiClient;
import com.procesadoraperu.inventario.data.local.database.InventarioDatabase;
import com.procesadoraperu.inventario.data.local.dao.SucursalDao;
import com.procesadoraperu.inventario.data.local.dao.UsuarioDao;
import com.procesadoraperu.inventario.data.remote.api.AuthApi;
import com.procesadoraperu.inventario.data.remote.api.SucursalApi;
import com.procesadoraperu.inventario.data.repository.AuthRepositoryImpl;
import com.procesadoraperu.inventario.data.repository.SucursalRepositoryImpl;
import com.procesadoraperu.inventario.domain.repository.auth.IAuthRepository;
import com.procesadoraperu.inventario.domain.repository.sucursal.ISucursalRepository;
import com.procesadoraperu.inventario.domain.usecase.auth.LoginUseCase;
import com.procesadoraperu.inventario.domain.usecase.auth.LogoutUseCase;
import com.procesadoraperu.inventario.domain.usecase.sucursal.GetSucursalesUseCase;
import com.procesadoraperu.inventario.presentation.auth.LoginViewModel;
import com.procesadoraperu.inventario.presentation.home.HomeViewModel;

import retrofit2.Retrofit;

public class ViewModelFactory implements ViewModelProvider.Factory {

    private final Context context;

    // Pedimos el Contexto porque Room (SQLite) y SharedPreferences lo necesitan
    public ViewModelFactory(Context context) {
        this.context = context.getApplicationContext(); // Evita fugas de memoria
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {

        // =====================================================================
        // 1. Instanciar el "Core" (Motor de BD, Red y Preferencias)
        // =====================================================================
        InventarioDatabase db = InventarioDatabase.getInstance(context);
        Retrofit retrofit = ApiClient.getClient(context);

        // NUEVO: Instanciamos SharedPreferences para el estado de la app
        SharedPreferences appPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);

        // =====================================================================
        // 2. Extraer DAOs y APIs
        // =====================================================================
        UsuarioDao usuarioDao = db.usuarioDao();
        SucursalDao sucursalDao = db.sucursalDao();

        AuthApi authApi = retrofit.create(AuthApi.class);
        SucursalApi sucursalApi = retrofit.create(SucursalApi.class);

        // =====================================================================
        // 3. Construir los Repositorios (Capa Data)
        // =====================================================================
        IAuthRepository authRepository = new AuthRepositoryImpl(authApi, usuarioDao, context);

        // CORREGIDO: Ahora pasamos los 3 argumentos que pide el Repositorio
        ISucursalRepository sucursalRepository = new SucursalRepositoryImpl(sucursalApi, sucursalDao, appPrefs);

        // =====================================================================
        // 4. Devolver el ViewModel solicitado con sus UseCases inyectados
        // =====================================================================
        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            LoginUseCase loginUseCase = new LoginUseCase(authRepository);
            return (T) new LoginViewModel(loginUseCase);

        } else if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            GetSucursalesUseCase getSucursalesUseCase = new GetSucursalesUseCase(sucursalRepository);
            LogoutUseCase logoutUseCase = new LogoutUseCase(authRepository);

            // Ojo: Cuando construyas el HomeViewModel completo, seguramente necesitarás
            // inyectarle también SearchSucursalUseCase y SaveActiveSucursalUseCase aquí.
            return (T) new HomeViewModel(getSucursalesUseCase, logoutUseCase);
        }

        throw new IllegalArgumentException("Clase ViewModel desconocida: " + modelClass.getName());
    }
}