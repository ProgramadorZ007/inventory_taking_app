package com.procesadoraperu.inventario;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.procesadoraperu.inventario.data.remote.api.InventarioApi;
import com.procesadoraperu.inventario.data.remote.request.RegistrarInventarioRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class InventoryIntegrationTest {

    private MockWebServer mockWebServer;
    private InventarioApi inventarioApi;

    @Before
    public void setup() throws Exception {
        // Levantamos un servidor local real
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Configuramos Retrofit para que "crea" que el servidor de la empresa es nuestro servidor local
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mockWebServer.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        inventarioApi = retrofit.create(InventarioApi.class);
    }

    // PRUEBA EN ANCHURA: Validar que la API maneja correctamente una respuesta 200 OK
    @Test
    public void testIntegration_Anchura_RespuestaExitosa() throws Exception {
        // Simulamos que el servidor real responde con éxito
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"success\":true, \"message\":\"Sincronización completa\"}"));

        RegistrarInventarioRequest request = new RegistrarInventarioRequest();
        request.idProducto = "P-001";
        request.cantidad = 10.0;

        // EJECUCIÓN REAL de la llamada de red
        Response<Void> response = inventarioApi.registrarInventario(request).execute();

        // VALIDACIÓN: ¿La integración entre Retrofit y el Servidor funcionó?
        assertEquals("La conexión falló, no se obtuvo 200 OK", 200, response.code());
        System.out.println("ANCHURA: Conexión establecida con éxito con el servidor local.");
    }

    // PRUEBA EN PROFUNDIDAD: Validar cómo reacciona la integración ante un error de servidor (500)
    @Test
    public void testIntegration_Profundidad_ErrorServidor() throws Exception {
        // Simulamos que el servidor tiene un error interno
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{\"success\":false, \"message\":\"Error interno en el servidor\"}"));

        RegistrarInventarioRequest request = new RegistrarInventarioRequest();

        Response<Void> response = inventarioApi.registrarInventario(request).execute();

        // VALIDACIÓN: El sistema debe detectar el código 500
        assertEquals("El sistema debería haber detectado un error 500", 500, response.code());
        System.out.println("PROFUNDIDAD: La integración manejó correctamente el error 500 del backend.");
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }
}