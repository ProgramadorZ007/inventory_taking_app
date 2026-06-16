package com.procesadoraperu.inventario;

import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.procesadoraperu.inventario.data.remote.request.RegistrarInventarioRequest;
import org.junit.Test;

public class InventoryRegressionTest {

    @Test
    public void testRegression_SyncWithAuditInfo() {
        Gson gson = new Gson();

        // Simulación de los datos originales
        RegistrarInventarioRequest request = new RegistrarInventarioRequest();
        request.idProducto = "REG-99";
        request.cantidad = 5.0;

        // Simulación del cambio reciente
        RegistrarInventarioRequest.AuditInfo audit = new RegistrarInventarioRequest.AuditInfo();
        audit.dispositivo = "Android 13";
        audit.latitud = "-12.046374";
        audit.longitud = "-77.042793";
        request.auditClientInfo = audit;

        String jsonResult = gson.toJson(request);

        // VALIDACIÓN DE REGRESIÓN:
        // 1. ¿Sigue funcionando lo viejo?
        assertTrue("REGRESIÓN FALLIDA: El ID del producto se perdió tras el cambio",
                jsonResult.contains("\"idProducto\":\"REG-99\""));

        // 2. ¿Lo nuevo se integró correctamente sin dañar el resto?
        assertTrue("REGRESIÓN FALLIDA: El nuevo campo de auditoría no se incluyó",
                jsonResult.contains("\"dispositivo\":\"Android 13\""));

        System.out.println("PRUEBA DE REGRESIÓN EXITOSA: La funcionalidad base se mantiene tras agregar auditoría.");
        System.out.println("JSON FINAL: " + jsonResult);
    }
}
