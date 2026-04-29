package com.procesadoraperu.inventario.data.repository;

import android.content.SharedPreferences;

import com.procesadoraperu.inventario.data.local.dao.SucursalDao;
import com.procesadoraperu.inventario.data.local.entity.SucursalEntity;
import com.procesadoraperu.inventario.data.remote.api.SucursalApi;
import com.procesadoraperu.inventario.data.remote.response.BaseResponse;
import com.procesadoraperu.inventario.domain.model.sucursal.Sucursal;
import com.procesadoraperu.inventario.domain.repository.sucursal.ISucursalRepository;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class SucursalRepositoryImpl implements ISucursalRepository {

    private final SucursalApi sucursalApi;
    private final SucursalDao sucursalDao;
    private final SharedPreferences prefs; // NUEVO: Necesario para guardar el estado

    private static final String PREF_ACTIVE_SUCURSAL = "ACTIVE_SUCURSAL_ID";

    // Inyectamos la API, el DAO y las Preferencias
    public SucursalRepositoryImpl(SucursalApi sucursalApi, SucursalDao sucursalDao, SharedPreferences prefs) {
        this.sucursalApi = sucursalApi;
        this.sucursalDao = sucursalDao;
        this.prefs = prefs;
    }

    // ==========================================================
    // 1. OPERACIONES DE RED Y BASE DE DATOS (Lo que ya tenías)
    // ==========================================================

    @Override
    public List<Sucursal> fetchSucursalesRemote() throws Exception {
        // Ejecución síncrona, Retrofit puede lanzar IOException aquí
        Response<BaseResponse<List<SucursalEntity>>> response = sucursalApi.getSucursales().execute();

        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
            List<SucursalEntity> entities = response.body().getData();
            return mapListToDomain(entities); // Convertimos Entity a Dominio
        } else {
            throw new Exception("Error al obtener sucursales del servidor");
        }
    }

    @Override
    public List<Sucursal> getSucursalesLocal() {
        List<SucursalEntity> entities = sucursalDao.getAll();
        return mapListToDomain(entities);
    }

    @Override
    public void saveSucursalesLocal(List<Sucursal> sucursales) {
        List<SucursalEntity> entities = new ArrayList<>();
        for (Sucursal s : sucursales) {
            SucursalEntity entity = new SucursalEntity();
            entity.idSucursal = s.getIdSucursal();
            entity.descripcion = s.getDescripcion();
            entities.add(entity);
        }
        sucursalDao.refreshData(entities); // Usa la transacción de Room (Borra e inserta)
    }

    // ==========================================================
    // 2. OPERACIONES DE ESTADO DE SESIÓN (¡Lo que faltaba!)
    // ==========================================================

    @Override
    public void saveActiveSucursalId(String idSucursal) {
        // Guarda el ID en la memoria del teléfono
        prefs.edit().putString(PREF_ACTIVE_SUCURSAL, idSucursal).apply();
    }

    @Override
    public String getActiveSucursalId() {
        // Recupera el ID. Retorna null si el operario aún no elige nada.
        return prefs.getString(PREF_ACTIVE_SUCURSAL, null);
    }

    // ==========================================================
    // 3. MAPPERS
    // ==========================================================

    // Esto asegura que la capa de dominio solo reciba objetos puros
    private List<Sucursal> mapListToDomain(List<SucursalEntity> entities) {
        List<Sucursal> list = new ArrayList<>();
        if (entities != null) {
            for (SucursalEntity entity : entities) {
                list.add(new Sucursal(entity.idSucursal, entity.descripcion));
            }
        }
        return list;
    }
}