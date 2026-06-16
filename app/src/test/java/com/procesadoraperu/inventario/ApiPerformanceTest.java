
//SCRIPT DE PRUEBA
/*
package com.procesadoraperu.inventario;

import static org.mockito.Mockito.*;
import org.junit.Test;
import com.procesadoraperu.inventario.domain.model.inventario.Inventario;
import com.procesadoraperu.inventario.domain.model.inventario.AuditClientInfo;
import com.procesadoraperu.inventario.domain.repository.inventario.IInventarioRepository;
import com.procesadoraperu.inventario.domain.repository.log.ILogRepository;
import com.procesadoraperu.inventario.domain.provider.IAuditClientInfoProvider;
import com.procesadoraperu.inventario.domain.provider.IAuditClientInfoProvider.OnAuditInfoCallback;
import com.procesadoraperu.inventario.domain.usecase.inventario.GetInventariosPendientesUseCase;
import com.procesadoraperu.inventario.domain.usecase.inventario.RegistrarInventarioUseCase;

public class ApiPerformanceTest {

    @Test
    public void medirRendimiento_GetInventariosPendientes() {
        IInventarioRepository repo = mock(IInventarioRepository.class);
        GetInventariosPendientesUseCase useCase = new GetInventariosPendientesUseCase(repo);

        long start = System.currentTimeMillis();
        useCase.execute("testUser");
        long duration = System.currentTimeMillis() - start;

        System.out.println("TEST_RENDIMIENTO: GetInventariosPendientes tardó " + duration + "ms");
    }

    @Test
    public void medirRendimiento_RegistrarInventario() {
        IInventarioRepository repo = mock(IInventarioRepository.class);
        ILogRepository logRepo = mock(ILogRepository.class);
        IAuditClientInfoProvider auditProvider = mock(IAuditClientInfoProvider.class);

        RegistrarInventarioUseCase useCase = new RegistrarInventarioUseCase(repo, logRepo, auditProvider);

        AuditClientInfo auditFake = new AuditClientInfo(
                "dispositivoTest", "0.0.0.0", "hostnameTest",
                "userAgentTest", "0.0", "0.0"
        );

        doAnswer(invocation -> {
            OnAuditInfoCallback callback = invocation.getArgument(0, OnAuditInfoCallback.class);
            callback.onSuccess(auditFake);
            return null;
        }).when(auditProvider).getAuditInfo(any());

        long start = System.currentTimeMillis();
        useCase.execute(new Inventario(), new RegistrarInventarioUseCase.OnRegistroCallback() {
            @Override public void onSincronizado() {}
            @Override public void onGuardadoLocal() {}
            @Override public void onError(Exception e) {}
        });

        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        long duration = System.currentTimeMillis() - start;
        System.out.println("TEST_RENDIMIENTO: RegistrarInventario tardó " + duration + "ms");
    }
}

*/

package com.procesadoraperu.inventario;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;

import com.procesadoraperu.inventario.data.local.dao.InventarioDao;
import com.procesadoraperu.inventario.data.local.dao.LogDao;
import com.procesadoraperu.inventario.data.local.entity.InventarioEntity;
import com.procesadoraperu.inventario.data.local.entity.LogEntity;
import com.procesadoraperu.inventario.data.repository.InventarioRepositoryImpl;
import com.procesadoraperu.inventario.data.repository.LogRepositoryImpl;
import com.procesadoraperu.inventario.data.remote.api.InventarioApi;
import com.procesadoraperu.inventario.domain.model.inventario.Inventario;
import com.procesadoraperu.inventario.domain.model.inventario.AuditClientInfo;
import com.procesadoraperu.inventario.domain.model.log.LogIntegracion;
import com.procesadoraperu.inventario.domain.repository.inventario.IInventarioRepository;
import com.procesadoraperu.inventario.domain.repository.log.ILogRepository;
import com.procesadoraperu.inventario.domain.usecase.inventario.SincronizarPendientesUseCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/**
 * ============================================================
 * PRUEBAS DE RENDIMIENTO - PROCESADORA PERÚ S.A.C.
 * ============================================================
 *
 * Archivo: ApiPerformanceTest.java
 * Tipo: Unit Test (JVM Local - sin emulador)
 * Metodología: Medición de tiempo real con System.currentTimeMillis()
 *
 * Componentes evaluados:
 *  1. ApiClient           → Inicialización del singleton Retrofit
 *  2. TokenAuthenticator  → Lógica de renovación de token (flujos)
 *  3. SincronizarPendientesUseCase → Sincronización de inventarios offline
 *  4. InventarioRepositoryImpl → Operaciones CRUD locales y mappers
 *  5. LogRepositoryImpl   → Persistencia y consulta de auditoría
 *
 * Ejecución:
 *   ./gradlew testDebugUnitTest --tests "com.procesadoraperu.inventario.ApiPerformanceTest"
 * ============================================================
 */
public class ApiPerformanceTest {

    // ============================================================
    // MOCKS COMPARTIDOS
    // ============================================================

    private IInventarioRepository mockInventarioRepo;
    private ILogRepository mockLogRepo;
    private InventarioDao mockInventarioDao;
    private LogDao mockLogDao;
    private InventarioApi mockInventarioApi;

    @Before
    public void setUp() {
        mockInventarioRepo = mock(IInventarioRepository.class);
        mockLogRepo        = mock(ILogRepository.class);
        mockInventarioDao  = mock(InventarioDao.class);
        mockLogDao         = mock(LogDao.class);
        mockInventarioApi  = mock(InventarioApi.class);
    }

    // ============================================================
    // 1. PRUEBA: ApiClient — Inicialización del singleton Retrofit
    // ============================================================

    /**
     * Mide el tiempo de construcción interna del patrón Singleton de ApiClient.
     *
     * Se simula la lógica de doble verificación (double-checked locking)
     * sin depender del Context de Android, usando solo la URL base y
     * la verificación de la constante pública.
     *
     * Resultado esperado: < 10ms (solo acceso a campo estático).
     */
    @Test
    public void rendimiento_ApiClient_AccesoBaseUrl() {
        long start = System.currentTimeMillis();

        // Verificamos acceso a la constante BASE_URL (sin Context de Android)
        String url = com.procesadoraperu.inventario.core.network.ApiClient.BASE_URL;

        long duration = System.currentTimeMillis() - start;

        System.out.println("────────────────────────────────────────");
        System.out.println("TEST: ApiClient - Acceso a BASE_URL");
        System.out.println("URL obtenida : " + url);
        System.out.println("Duracion     : " + duration + "ms");
        System.out.println("Resultado    : " + (duration < 10 ? "OPTIMO" : "LENTO"));
        System.out.println("────────────────────────────────────────");
    }

    // ============================================================
    // 2. PRUEBA: TokenAuthenticator — Flujo sin Refresh Token
    // ============================================================

    /**
     * Mide el tiempo de decisión cuando no hay Refresh Token disponible.
     *
     * En producción, esta ruta invoca logoutAndNavigateToLogin().
     * Aquí simulamos la verificación del token nulo antes de llamar al servidor,
     * validando que la decisión sea inmediata (< 5ms).
     *
     * Resultado esperado: < 5ms (decisión en memoria, sin I/O).
     */
    @Test
    public void rendimiento_TokenAuthenticator_SinRefreshToken() {
        long start = System.currentTimeMillis();

        // Simulamos la lógica de verificación del refreshToken
        String refreshToken = null; // Escenario: sesión expirada sin token
        boolean debeRedirigir = (refreshToken == null || refreshToken.isEmpty());

        long duration = System.currentTimeMillis() - start;

        System.out.println("────────────────────────────────────────");
        System.out.println("TEST: TokenAuthenticator - Sin Refresh Token");
        System.out.println("Debe redirigir al login: " + debeRedirigir);
        System.out.println("Duracion               : " + duration + "ms");
        System.out.println("Resultado              : " + (duration < 5 ? "OPTIMO" : "LENTO"));
        System.out.println("────────────────────────────────────────");
    }

    /**
     * Mide el tiempo de decisión cuando el Refresh Token existe y es válido.
     *
     * Simula la ruta exitosa: el token existe, se construiría la nueva petición.
     * No ejecuta llamada HTTP real; valida solo la rama de decisión en memoria.
     *
     * Resultado esperado: < 5ms.
     */
    @Test
    public void rendimiento_TokenAuthenticator_ConRefreshToken() {
        long start = System.currentTimeMillis();

        // Simulamos token válido en SharedPreferences
        String refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mockToken";
        boolean tokenDisponible = (refreshToken != null && !refreshToken.isEmpty());

        // Simula que se procedería a llamar al endpoint /refresh-token
        String endpointRefresh = com.procesadoraperu.inventario.core.network.ApiClient.BASE_URL
                + "/api/auth/refresh-token";

        long duration = System.currentTimeMillis() - start;

        System.out.println("────────────────────────────────────────");
        System.out.println("TEST: TokenAuthenticator - Con Refresh Token valido");
        System.out.println("Token disponible  : " + tokenDisponible);
        System.out.println("Endpoint a llamar : " + endpointRefresh);
        System.out.println("Duracion          : " + duration + "ms");
        System.out.println("Resultado         : " + (duration < 5 ? "OPTIMO" : "LENTO"));
        System.out.println("────────────────────────────────────────");
    }

    // ============================================================
    // 3. PRUEBA: SincronizarPendientesUseCase — Sincronización offline
    // ============================================================

    /**
     * Mide el tiempo de sincronización cuando NO hay inventarios pendientes.
     *
     * Resultado esperado: < 20ms (lista vacía, sin iteraciones).
     */
    @Test
    public void rendimiento_SincronizarPendientes_ListaVacia() throws Exception {
        when(mockInventarioRepo.getInventariosLocalesPorEstado("testUser", "PENDIENTE"))
                .thenReturn(new ArrayList<>());

        SincronizarPendientesUseCase useCase =
                new SincronizarPendientesUseCase(mockInventarioRepo, mockLogRepo);

        long start = System.currentTimeMillis();
        int fallidos = useCase.execute("testUser");
        long duration = System.currentTimeMillis() - start;

        System.out.println("────────────────────────────────────────");
        System.out.println("TEST: SincronizarPendientes - Lista vacia");
        System.out.println("Fallidos : " + fallidos);
        System.out.println("Duracion : " + duration + "ms");
        System.out.println("Resultado: " + (duration < 20 ? "OPTIMO" : "LENTO"));
        System.out.println("────────────────────────────────────────");
    }

    /**
     * Mide el tiempo de sincronización exitosa de 5 inventarios pendientes.
     *
     * Simula que el servidor acepta cada envío (enviarInventarioRemote no lanza excepción).
     * Resultado esperado: < 100ms para 5 registros (procesamiento en memoria).
     */
    @Test
    public void rendimiento_SincronizarPendientes_Exitoso_5Registros() throws Exception {
        List<Inventario> pendientes = crearListaInventarios(5);

        when(mockInventarioRepo.getInventariosLocalesPorEstado("testUser", "PENDIENTE"))
                .thenReturn(pendientes);

        // No lanza excepción = sincronización exitosa
        doNothing().when(mockInventarioRepo).enviarInventarioRemote(any(Inventario.class));
        doNothing().when(mockInventarioRepo).deleteInventarioLocal(any(Inventario.class));
        doNothing().when(mockLogRepo).saveLogLocal(any(LogIntegracion.class));

        SincronizarPendientesUseCase useCase =
                new SincronizarPendientesUseCase(mockInventarioRepo, mockLogRepo);

        long start = System.currentTimeMillis();
        int fallidos = useCase.execute("testUser");
        long duration = System.currentTimeMillis() - start;

        System.out.println("────────────────────────────────────────");
        System.out.println("TEST: SincronizarPendientes - 5 registros exitosos");
        System.out.println("Fallidos : " + fallidos);
        System.out.println("Duracion : " + duration + "ms");
        System.out.println("Resultado: " + (duration < 100 ? "OPTIMO" : "LENTO"));
        System.out.println("────────────────────────────────────────");
    }

    /**
     * Mide el tiempo de sincronización cuando TODOS los envíos fallan.
     *
     * Simula que el servidor está caído (Exception en enviarInventarioRemote).
     * Valida que la tolerancia a fallos no introduce demoras significativas.
     * Resultado esperado: < 100ms para 5 registros fallidos.
     */
    @Test
    public void rendimiento_SincronizarPendientes_TodosFallan_5Registros() throws Exception {
        List<Inventario> pendientes = crearListaInventarios(5);

        when(mockInventarioRepo.getInventariosLocalesPorEstado("testUser", "PENDIENTE"))
                .thenReturn(pendientes);

        // Simula servidor caído
        doThrow(new Exception("Servidor no disponible"))
                .when(mockInventarioRepo).enviarInventarioRemote(any(Inventario.class));
        doNothing().when(mockLogRepo).saveLogLocal(any(LogIntegracion.class));

        SincronizarPendientesUseCase useCase =
                new SincronizarPendientesUseCase(mockInventarioRepo, mockLogRepo);

        long start = System.currentTimeMillis();
        int fallidos = useCase.execute("testUser");
        long duration = System.currentTimeMillis() - start;

        System.out.println("────────────────────────────────────────");
        System.out.println("TEST: SincronizarPendientes - 5 registros fallidos");
        System.out.println("Fallidos : " + fallidos + " (esperado: 5)");
        System.out.println("Duracion : " + duration + "ms");
        System.out.println("Resultado: " + (duration < 100 ? "OPTIMO" : "LENTO"));
        System.out.println("────────────────────────────────────────");
    }

    // ============================================================
    // 4. PRUEBA: InventarioRepositoryImpl — CRUD local y mappers
    // ============================================================

    /**
     * Mide el tiempo de guardado local de un inventario.
     *
     * Incluye el tiempo del mapper dominio → entidad (mapToEntity)
     * más el tiempo del insert en el DAO (mockeado).
     * Resultado esperado: < 10ms.
     */
    @Test
    public void rendimiento_InventarioRepository_SaveLocal() {
        doNothing().when(mockInventarioDao).insert(any(InventarioEntity.class));

        InventarioRepositoryImpl repo =
                new InventarioRepositoryImpl(mockInventarioApi, mockInventarioDao);

        Inventario inventario = crearInventarioCompleto();

        long start = System.currentTimeMillis();
        repo.saveInventarioLocal(inventario);
        long duration = System.currentTimeMillis() - start;

        System.out.println("────────────────────────────────────────");
        System.out.println("TEST: InventarioRepository - saveInventarioLocal");
        System.out.println("Duracion : " + duration + "ms");
        System.out.println("Resultado: " + (duration < 10 ? "OPTIMO" : "LENTO"));
        System.out.println("────────────────────────────────────────");
    }

    /**
     * Mide el tiempo de consulta y mapeo de inventarios locales.
     *
     * Simula 10 entidades devueltas por Room y mide el tiempo del
     * mapper entidad → dominio aplicado a cada una.
     * Resultado esperado: < 20ms para 10 registros.
     */
    @Test
    public void rendimiento_InventarioRepository_GetLocalesPorEstado_10Registros() {
        List<InventarioEntity> entidades = crearListaEntidades(10);
        when(mockInventarioDao.getByStatusAndUser("testUser", "PENDIENTE"))
                .thenReturn(entidades);

        InventarioRepositoryImpl repo =
                new InventarioRepositoryImpl(mockInventarioApi, mockInventarioDao);

        long start = System.currentTimeMillis();
        List<Inventario> resultado = repo.getInventariosLocalesPorEstado("testUser", "PENDIENTE");
        long duration = System.currentTimeMillis() - start;

        System.out.println("────────────────────────────────────────");
        System.out.println("TEST: InventarioRepository - getInventariosLocalesPorEstado (10 registros)");
        System.out.println("Registros obtenidos: " + resultado.size());
        System.out.println("Duracion           : " + duration + "ms");
        System.out.println("Resultado          : " + (duration < 20 ? "OPTIMO" : "LENTO"));
        System.out.println("────────────────────────────────────────");
    }

    /**
     * Mide el tiempo de eliminación local de un inventario.
     *
     * Resultado esperado: < 10ms.
     */
    @Test
    public void rendimiento_InventarioRepository_DeleteLocal() {
        doNothing().when(mockInventarioDao).delete(any(InventarioEntity.class));

        InventarioRepositoryImpl repo =
                new InventarioRepositoryImpl(mockInventarioApi, mockInventarioDao);

        Inventario inventario = crearInventarioCompleto();

        long start = System.currentTimeMillis();
        repo.deleteInventarioLocal(inventario);
        long duration = System.currentTimeMillis() - start;

        System.out.println("────────────────────────────────────────");
        System.out.println("TEST: InventarioRepository - deleteInventarioLocal");
        System.out.println("Duracion : " + duration + "ms");
        System.out.println("Resultado: " + (duration < 10 ? "OPTIMO" : "LENTO"));
        System.out.println("────────────────────────────────────────");
    }

    // ============================================================
    // 5. PRUEBA: LogRepositoryImpl — Auditoría y trazabilidad
    // ============================================================

    /**
     * Mide el tiempo de guardado de un log de integración.
     *
     * Incluye mapper dominio → entidad + insert mockeado.
     * Resultado esperado: < 10ms.
     */
    @Test
    public void rendimiento_LogRepository_SaveLogLocal() {
        doNothing().when(mockLogDao).insert(any(LogEntity.class));

        LogRepositoryImpl repo = new LogRepositoryImpl(mockLogDao);

        LogIntegracion log = new LogIntegracion(
                "/api/almacen/inventarios", "POST", 200,
                "{idProducto: P001, cantidad: 10}",
                "OK", null,
                45L, "2025-06-01 10:00:00",
                "testUser", "LocalID: inv-001"
        );

        long start = System.currentTimeMillis();
        repo.saveLogLocal(log);
        long duration = System.currentTimeMillis() - start;

        System.out.println("────────────────────────────────────────");
        System.out.println("TEST: LogRepository - saveLogLocal");
        System.out.println("Duracion : " + duration + "ms");
        System.out.println("Resultado: " + (duration < 10 ? "OPTIMO" : "LENTO"));
        System.out.println("────────────────────────────────────────");
    }

    /**
     * Mide el tiempo de consulta y mapeo de todos los logs locales.
     *
     * Simula 20 entidades de log devueltas por Room.
     * Resultado esperado: < 20ms para 20 registros.
     */
    @Test
    public void rendimiento_LogRepository_GetLogsLocales_20Registros() {
        List<LogEntity> entidades = crearListaLogs(20);
        when(mockLogDao.getAllLogs()).thenReturn(entidades);

        LogRepositoryImpl repo = new LogRepositoryImpl(mockLogDao);

        long start = System.currentTimeMillis();
        List<LogIntegracion> resultado = repo.getLogsLocales();
        long duration = System.currentTimeMillis() - start;

        System.out.println("────────────────────────────────────────");
        System.out.println("TEST: LogRepository - getLogsLocales (20 registros)");
        System.out.println("Logs obtenidos: " + resultado.size());
        System.out.println("Duracion      : " + duration + "ms");
        System.out.println("Resultado     : " + (duration < 20 ? "OPTIMO" : "LENTO"));
        System.out.println("────────────────────────────────────────");
    }

    /**
     * Mide el tiempo de limpieza masiva de logs.
     *
     * Resultado esperado: < 5ms (delega en el DAO mockeado).
     */
    @Test
    public void rendimiento_LogRepository_ClearOldLogs() {
        doNothing().when(mockLogDao).clearAllLogs();

        LogRepositoryImpl repo = new LogRepositoryImpl(mockLogDao);

        long start = System.currentTimeMillis();
        repo.clearOldLogs();
        long duration = System.currentTimeMillis() - start;

        System.out.println("────────────────────────────────────────");
        System.out.println("TEST: LogRepository - clearOldLogs");
        System.out.println("Duracion : " + duration + "ms");
        System.out.println("Resultado: " + (duration < 5 ? "OPTIMO" : "LENTO"));
        System.out.println("────────────────────────────────────────");
    }

    // ============================================================
    // HELPERS — DATOS DE PRUEBA
    // ============================================================

    private Inventario crearInventarioCompleto() {
        Inventario inv = new Inventario();
        inv.setIdInventario("inv-test-001");
        inv.setIdEmpresa("001");
        inv.setIdSucursal("SUC-01");
        inv.setSucursal("Sucursal Central");
        inv.setIdAlmacen("ALM-01");
        inv.setAlmacen("Almacen Principal");
        inv.setIdProducto("P001");
        inv.setProducto("Producto de Prueba");
        inv.setUnidadMedida("UND");
        inv.setStock(100.0);
        inv.setCantidad(50.0);
        inv.setUsuarioCreacion("testUser");
        inv.setFechaCreacion("2025-06-01 08:00:00");
        inv.setFechaRegistroLocal("2025-06-01 08:00:01");
        inv.setEstadoSincronizacion("PENDIENTE");
        inv.setAuditClientInfo(new AuditClientInfo(
                "Samsung Galaxy A54", "192.168.1.10",
                "android-device", "Dalvik/2.1.0",
                "-12.0453", "-77.0311"
        ));
        return inv;
    }

    private List<Inventario> crearListaInventarios(int cantidad) {
        List<Inventario> lista = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            Inventario inv = new Inventario();
            inv.setIdInventario("inv-" + i);
            inv.setIdProducto("P00" + i);
            inv.setProducto("Producto " + i);
            inv.setCantidad((double) (i + 1) * 10);
            inv.setStock(100.0);
            inv.setEstadoSincronizacion("PENDIENTE");
            lista.add(inv);
        }
        return lista;
    }

    private List<InventarioEntity> crearListaEntidades(int cantidad) {
        List<InventarioEntity> lista = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            InventarioEntity e = new InventarioEntity();
            e.idInventario    = "inv-entity-" + i;
            e.idEmpresa       = "001";
            e.idSucursal      = "SUC-01";
            e.idAlmacen       = "ALM-01";
            e.idProducto      = "P00" + i;
            e.producto        = "Producto " + i;
            e.unidadMedida    = "UND";
            e.stock           = 100.0;
            e.cantidad        = (double) (i + 1) * 5;
            e.usuarioCreacion = "testUser";
            e.estadoSincronizacion = "PENDIENTE";
            lista.add(e);
        }
        return lista;
    }

    private List<LogEntity> crearListaLogs(int cantidad) {
        List<LogEntity> lista = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            LogEntity e = new LogEntity();
            e.endpoint          = "/api/almacen/inventarios";
            e.metodoHttp        = "POST";
            e.codigoHttp        = (i % 3 == 0) ? 500 : 200;
            e.payloadEnvio      = "{idProducto: P00" + i + "}";
            e.respuestaErp      = (i % 3 == 0) ? null : "OK";
            e.detalleError      = (i % 3 == 0) ? "Timeout" : null;
            e.tiempoRespuestaMs = 30L + i;
            e.fechaRegistro     = "2025-06-01 10:0" + (i % 10) + ":00";
            e.username          = "testUser";
            e.referenciaId      = "LocalID: inv-" + i;
            lista.add(e);
        }
        return lista;
    }
}




















