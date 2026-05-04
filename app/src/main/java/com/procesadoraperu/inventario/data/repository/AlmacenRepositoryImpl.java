package com.procesadoraperu.inventario.data.repository;

import android.content.SharedPreferences;

import com.procesadoraperu.inventario.data.local.dao.AlmacenDao;
import com.procesadoraperu.inventario.data.local.entity.AlmacenEntity;
import com.procesadoraperu.inventario.data.remote.api.AlmacenApi;
import com.procesadoraperu.inventario.data.remote.response.BaseResponse;
import com.procesadoraperu.inventario.domain.model.almacen.Almacen;
import com.procesadoraperu.inventario.domain.repository.almacen.IAlmacenRepository;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class AlmacenRepositoryImpl implements IAlmacenRepository {

    private final AlmacenApi almacenApi;
    private final AlmacenDao almacenDao;
    private final SharedPreferences prefs;

    private static final String PREF_ACTIVE_ALMACEN_ID = "ACTIVE_ALMACEN_ID";
    private static final String PREF_ACTIVE_ALMACEN_SUC = "ACTIVE_ALMACEN_SUC";
    private static final String PREF_ACTIVE_ALMACEN_DESC = "ACTIVE_ALMACEN_DESC";

    public AlmacenRepositoryImpl(AlmacenApi almacenApi, AlmacenDao almacenDao, SharedPreferences prefs) {
        this.almacenApi = almacenApi;
        this.almacenDao = almacenDao;
        this.prefs = prefs;
    }

    @Override
    public List<Almacen> fetchAlmacenesRemote(String idSucursal) throws Exception {
        // Retrofit automáticamente reemplazará {idSucursal} por el valor de la variable
        Response<BaseResponse<List<AlmacenEntity>>> response = almacenApi.getAlmacenes(idSucursal).execute();

        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
            List<AlmacenEntity> entities = response.body().getData();
            return mapListToDomain(entities);
        } else {
            throw new Exception("Error al obtener almacenes para la sucursal " + idSucursal);
        }
    }

    @Override
    public List<Almacen> getAlmacenesLocal(String idSucursal) {
        List<AlmacenEntity> entities = almacenDao.getPorSucursal(idSucursal);
        return mapListToDomain(entities);
    }

    @Override
    public void saveAlmacenesLocal(List<Almacen> almacenes) {
        if (almacenes == null || almacenes.isEmpty()) return;

        // Extraemos la sucursal del primer elemento para hacer la limpieza
        String idSucursal = almacenes.get(0).getIdSucursal();

        List<AlmacenEntity> entities = new ArrayList<>();
        for (Almacen a : almacenes) {
            AlmacenEntity entity = new AlmacenEntity();
            entity.idAlmacen = a.getIdAlmacen();
            entity.idSucursal = a.getIdSucursal();
            entity.descripcion = a.getDescripcion();
            entities.add(entity);
        }

        almacenDao.refreshData(idSucursal, entities);
    }

    @Override
    public void saveActiveAlmacen(Almacen almacen) {
        prefs.edit()
                .putString(PREF_ACTIVE_ALMACEN_ID, almacen.getIdAlmacen())
                .putString(PREF_ACTIVE_ALMACEN_SUC, almacen.getIdSucursal())
                .putString(PREF_ACTIVE_ALMACEN_DESC, almacen.getDescripcion())
                .apply();
    }

    @Override
    public Almacen getActiveAlmacen() {
        String id = prefs.getString(PREF_ACTIVE_ALMACEN_ID, null);
        String idSuc = prefs.getString(PREF_ACTIVE_ALMACEN_SUC, null);
        String desc = prefs.getString(PREF_ACTIVE_ALMACEN_DESC, null);

        if (id != null && idSuc != null && desc != null) {
            return new Almacen(id, idSuc, desc);
        }
        return null;
    }

    // Mapper interno
    private List<Almacen> mapListToDomain(List<AlmacenEntity> entities) {
        List<Almacen> list = new ArrayList<>();
        if (entities != null) {
            for (AlmacenEntity entity : entities) {
                list.add(new Almacen(entity.idAlmacen, entity.idSucursal, entity.descripcion));
            }
        }
        return list;
    }
}