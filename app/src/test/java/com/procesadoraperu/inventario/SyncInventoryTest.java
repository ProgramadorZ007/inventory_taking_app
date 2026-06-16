package com.procesadoraperu.inventario;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import com.google.gson.Gson;
import com.procesadoraperu.inventario.data.remote.request.RegistrarInventarioRequest;
import com.procesadoraperu.inventario.data.remote.response.BaseResponse;
import org.junit.Test;

public class SyncInventoryTest {

    // --- PRUEBA DE CAJA BLANCA ---
    // Objetivo: Validar la estructura interna y serialización correcta de los campos.
    @Test
    public void testCajaBlanca_SerializacionCorrecta() {
        Gson gson = new Gson();
        RegistrarInventarioRequest request = new RegistrarInventarioRequest();
        request.idProducto = "PROD001";
        request.cantidad = 50.0;

        String jsonResult = gson.toJson(request);

        assertTrue("Error en Caja Blanca: Se esperaba que el JSON contuviera idProducto:PROD001. JSON obtenido: " +
                        jsonResult, jsonResult.contains("\"idProducto\":\"PROD001\""));

        double cantidadEsperada = 50.0;
        assertEquals("La cantidad serializada no coincide con la esperada",
                cantidadEsperada, request.cantidad, 0.0);
        
        assertTrue("Error en Caja Blanca: El JSON no contiene la cantidad correcta. JSON: " + jsonResult, 
                jsonResult.contains("\"cantidad\":50.0"));

        System.out.println("CAJA BLANCA: Test pasado. JSON generado: " + jsonResult);
    }

    // --- PRUEBA DE CAJA NEGRA (ÉXITO) ---
    // Objetivo: Validar funcionalidad de "Entrada y Salida" en escenario ideal.
    @Test
    public void testCajaNegra_DeserializacionRespuestaServidor() {
        Gson gson = new Gson();
        
        String jsonSimulado = "{\"success\":true,\"message\":\"Sincronizado correctamente\",\"data\":null}";
        String mensajeEsperado = "Sincronizado correctamente";

        BaseResponse response = gson.fromJson(jsonSimulado, BaseResponse.class);

        assertNotNull("La respuesta no debería ser nula", response);
        assertEquals("El mensaje recibido del servidor no es el esperado",
                mensajeEsperado, response.getMessage());
        assertTrue("El estado de éxito debería ser true", response.isSuccess());

        System.out.println("CAJA NEGRA (ÉXITO): Test pasado. Mensaje procesado: " +
                response.getMessage());
    }

    // --- PRUEBA DE CAJA NEGRA (ERROR) ---
    // Objetivo: Validar que el sistema reconoce cuando el servidor responde con un fallo.
    @Test
    public void testCajaNegra_ErrorEnRespuesta() {
        Gson gson = new Gson();

        // Simulamos un JSON de error proveniente del servidor
        // Entrada: Error de negocio (Ej: El producto ya no tiene stock)
        String jsonError = "{\"success\":false,\"message\":\"Error: Stock insuficiente\",\"data\":null}";
        
        // Acción: Deserializar el JSON
        BaseResponse response = gson.fromJson(jsonError, BaseResponse.class);

        // Validación (Salida): El sistema debe detectar que success es FALSE
        assertNotNull(response);
        assertFalse("El test falló: Se esperaba que 'success' fuera false ante un error del servidor",
                response.isSuccess());
        assertEquals("El mensaje de error no coincide con el enviado por el servidor", 
                "Error: Stock insuficiente", response.getMessage());

        System.out.println("CAJA NEGRA (ERROR): Test pasado. El sistema detectó correctamente el fallo: " +
                response.getMessage());
    }
}
