package com.procesadoraperu.inventario.presentation;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.procesadoraperu.inventario.core.network.ApiClient;
import com.procesadoraperu.inventario.data.local.database.InventarioDatabase;
import com.procesadoraperu.inventario.data.remote.api.AuthApi;
import com.procesadoraperu.inventario.data.remote.api.SucursalApi;
import com.procesadoraperu.inventario.data.remote.api.AlmacenApi;
import com.procesadoraperu.inventario.data.repository.AuthRepositoryImpl;
import com.procesadoraperu.inventario.data.repository.SucursalRepositoryImpl;
import com.procesadoraperu.inventario.data.repository.AlmacenRepositoryImpl;
import com.procesadoraperu.inventario.domain.repository.auth.IAuthRepository;
import com.procesadoraperu.inventario.domain.repository.sucursal.ISucursalRepository;
import com.procesadoraperu.inventario.domain.repository.almacen.IAlmacenRepository;
import com.procesadoraperu.inventario.domain.usecase.auth.LoginUseCase;
import com.procesadoraperu.inventario.domain.usecase.auth.LogoutUseCase;
import com.procesadoraperu.inventario.domain.usecase.sucursal.GetSucursalesUseCase;
import com.procesadoraperu.inventario.domain.usecase.almacen.GetAlmacenesUseCase;
import com.procesadoraperu.inventario.presentation.auth.LoginViewModel;
import com.procesadoraperu.inventario.presentation.home.HomeViewModel;
import com.procesadoraperu.inventario.presentation.selection.SelectionViewModel;

import retrofit2.Retrofit;

public class ViewModelFactory implements ViewModelProvider.Factory {

    private final Context context;

    public ViewModelFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        InventarioDatabase db = InventarioDatabase.getInstance(context);
        Retrofit retrofit = ApiClient.getClient(context);
        SharedPreferences appPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);

        // Repositorios
        IAuthRepository authRepo = new AuthRepositoryImpl(retrofit.create(AuthApi.class), db.usuarioDao(), context);
        ISucursalRepository sucRepo = new SucursalRepositoryImpl(retrofit.create(SucursalApi.class), db.sucursalDao(), appPrefs);
        IAlmacenRepository almRepo = new AlmacenRepositoryImpl(retrofit.create(AlmacenApi.class), db.almacenDao(), appPrefs);

        // Casos de Uso
        GetSucursalesUseCase getSucursalesUseCase = new GetSucursalesUseCase(sucRepo);
        GetAlmacenesUseCase getAlmacenesUseCase = new GetAlmacenesUseCase(almRepo);
        LogoutUseCase logoutUseCase = new LogoutUseCase(authRepo);

        // Inyección a ViewModels
        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            return (T) new LoginViewModel(new LoginUseCase(authRepo));

        } else if (modelClass.isAssignableFrom(SelectionViewModel.class)) {
            // Este ViewModel servirá tanto para buscar sucursales como almacenes
            return (T) new SelectionViewModel(getSucursalesUseCase, getAlmacenesUseCase, sucRepo, almRepo);

        } else if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            return (T) new HomeViewModel(logoutUseCase, authRepo, sucRepo, almRepo);
        }

        throw new IllegalArgumentException("ViewModel desconocido: " + modelClass.getName());
    }
}