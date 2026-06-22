package com.procesadoraperu.inventario.domain.usecase.inventario;

import com.procesadoraperu.inventario.domain.model.inventario.AuditClientInfo;
import com.procesadoraperu.inventario.domain.model.inventario.Inventario;
import com.procesadoraperu.inventario.domain.model.log.LogIntegracion;
import com.procesadoraperu.inventario.domain.provider.IAuditClientInfoProvider;
import com.procesadoraperu.inventario.domain.repository.inventario.IInventarioRepository;
import com.procesadoraperu.inventario.domain.repository.log.ILogRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests de Preservation Checking.
 *
 * Verifican que el fix no introduce regresiones: todos los comportamientos
 * existentes (campos del request, logs, callbacks, flujo sin GPS) se preservan.
 *
 * Validates: Requirements 3.1, 3.2, 3.4, 3.5
 */
public class RegistrarInventarioUseCasePreservationTest {

    private IInventarioRepository mockInventarioRepository;
    private ILogRepository mockLogRepository;
    private IAuditClientInfoProvider mockAuditProvider;
    private RegistrarInventarioUseCase useCase;

    private static final AuditClientInfo AUDIT_INFO = new AuditClientInfo(
            "TestDevice", "10.0.0.1", "test-host", "TestAgent/2.0", "-12.0464", "-77.0428"
    );

    private static final AuditClientInfo AUDIT_INFO_SIN_GPS = new AuditClientInfo(
            "TestDevice", "10.0.0.1", "test-host", "TestAgent/2.0", "", ""
    );

    @Before
    public void setUp() {
        mockInventarioRepository = mock(IInventarioRepository.class);
        mockLogRepository = mock(ILogRepository.class);
        mockAuditProvider = mock(IAuditClientInfoProvider.class);
        useCase = new RegistrarInventarioUseCase(
                mockInventarioRepository, mockLogRepository, mockAuditProvider
        );
    }

    /**
     * Configura el mock del provider para que retorne inmediatamente con el AuditClientInfo dado.
     */
    private void configurarProviderInmediato(AuditClientInfo auditInfo) {
        doAnswer(invocation -> {
            IAuditClientInfoProvider.OnAuditInfoCallback callback =
                    invocation.getArgument(0);
            callback.onSuccess(auditInfo);
            return null;
        }).when(mockAuditProvider).getAuditInfo(any());
    }

    /**
     * Construye un Inventario de prueba con todos los campos poblados.
     */
    private Inventario buildInventarioCompleto() {
        Inventario inv = new Inventario();
        inv.setIdEmpresa("EMP-001");
        inv.setIdSucursal("SUC-LIMA-01");
        inv.setIdAlmacen("ALM-CENTRAL");
        inv.setIdProducto("PROD-ARROZ-001");
        inv.setProducto("Arroz Extra");
        inv.setUnidadMedida("KG");
        inv.setStock(500.0);
        inv.setCantidad(25.5);
        inv.setUsuarioCreacion("operador01");
        return inv;
    }

    // ==========================================
    // TASK 7.2 — Todos los campos del request se envían correctamente
    // ==========================================

    /**
     * PRESERVATION: Todos los campos del request (idEmpresa, idSucursal, idAlmacen,
     * idProducto, dscProducto, idMedida, stock, cantidad) se envían correctamente
     * después del fix.
     *
     * Validates: Requirements 3.4
     */
    @Test
    public void preservation_todosLosCamposDelRequestSeEnvianCorrectamente()
            throws Exception {

        // Arrange
        configurarProviderInmediato(AUDIT_INFO);
        ArgumentCaptor<Inventario> inventarioCaptor = ArgumentCaptor.forClass(Inventario.class);
        CountDownLatch latch = new CountDownLatch(1);

        Inventario inventario = buildInventarioCompleto();

        // Act
        useCase.execute(inventario, new RegistrarInventarioUseCase.OnRegistroCallback() {
            @Override
            public void onSincronizado() { latch.countDown(); }

            @Override
            public void onGuardadoLocal() { latch.countDown(); }

            @Override
            public void onError(Exception e) { latch.countDown(); }
        });

        boolean completado = latch.await(3, TimeUnit.SECONDS);
        assertTrue("El flujo debe completar", completado);

        // Assert: verificar que todos los campos del inventario llegan al repositorio
        verify(mockInventarioRepository).enviarInventarioRemote(inventarioCaptor.capture());
        Inventario enviado = inventarioCaptor.getValue();

        assertEquals("idEmpresa debe preservarse", "EMP-001", enviado.getIdEmpresa());
        assertEquals("idSucursal debe preservarse", "SUC-LIMA-01", enviado.getIdSucursal());
        assertEquals("idAlmacen debe preservarse", "ALM-CENTRAL", enviado.getIdAlmacen());
        assertEquals("idProducto debe preservarse", "PROD-ARROZ-001", enviado.getIdProducto());
        assertEquals("dscProducto (producto) debe preservarse", "Arroz Extra", enviado.getProducto());
        assertEquals("idMedida (unidadMedida) debe preservarse", "KG", enviado.getUnidadMedida());
        assertEquals("stock debe preservarse", 500.0, enviado.getStock(), 0.001);
        assertEquals("cantidad debe preservarse", 25.5, enviado.getCantidad(), 0.001);
    }

    // ==========================================
    // TASK 7.3 — El log de integración se guarda correctamente en éxito y en error
    // ==========================================

    /**
     * PRESERVATION: El log de integración se guarda correctamente cuando el envío
     * al servidor es exitoso (código HTTP 200).
     *
     * Validates: Requirements 3.4
     */
    @Test
    public void preservation_logDeIntegracionSeGuardaCorrectamenteEnExito()
            throws Exception {

        // Arrange
        configurarProviderInmediato(AUDIT_INFO);
        ArgumentCaptor<LogIntegracion> logCaptor = ArgumentCaptor.forClass(LogIntegracion.class);
        CountDownLatch latch = new CountDownLatch(1);

        Inventario inventario = buildInventarioCompleto();

        // Act
        useCase.execute(inventario, new RegistrarInventarioUseCase.OnRegistroCallback() {
            @Override
            public void onSincronizado() { latch.countDown(); }

            @Override
            public void onGuardadoLocal() { latch.countDown(); }

            @Override
            public void onError(Exception e) { latch.countDown(); }
        });

        boolean completado = latch.await(3, TimeUnit.SECONDS);
        assertTrue("El flujo debe completar", completado);

        // Assert: el log de éxito debe guardarse con código 200
        verify(mockLogRepository).saveLogLocal(logCaptor.capture());
        LogIntegracion log = logCaptor.getValue();

        assertNotNull("El log no debe ser null", log);
        assertEquals("El endpoint debe ser correcto", "/api/almacen/inventarios", log.getEndpoint());
        assertEquals("El código HTTP debe ser 200 en éxito", 200, log.getCodigoHttp());
        assertNotNull("La fecha de registro debe estar poblada", log.getFechaRegistro());
        assertEquals("El username debe preservarse", "operador01", log.getUsername());
    }

    /**
     * PRESERVATION: El log de integración se guarda correctamente cuando el envío
     * al servidor falla (código HTTP 500, guardado local).
     *
     * Validates: Requirements 3.2
     */
    @Test
    public void preservation_logDeIntegracionSeGuardaCorrectamenteEnError()
            throws Exception {

        // Arrange
        configurarProviderInmediato(AUDIT_INFO);
        doThrow(new RuntimeException("Timeout de conexión"))
                .when(mockInventarioRepository).enviarInventarioRemote(any());

        ArgumentCaptor<LogIntegracion> logCaptor = ArgumentCaptor.forClass(LogIntegracion.class);
        CountDownLatch latch = new CountDownLatch(1);

        Inventario inventario = buildInventarioCompleto();

        // Act
        useCase.execute(inventario, new RegistrarInventarioUseCase.OnRegistroCallback() {
            @Override
            public void onSincronizado() { latch.countDown(); }

            @Override
            public void onGuardadoLocal() { latch.countDown(); }

            @Override
            public void onError(Exception e) { latch.countDown(); }
        });

        boolean completado = latch.await(3, TimeUnit.SECONDS);
        assertTrue("El flujo debe completar", completado);

        // Assert: el log de error debe guardarse con código 500
        verify(mockLogRepository).saveLogLocal(logCaptor.capture());
        LogIntegracion log = logCaptor.getValue();

        assertNotNull("El log no debe ser null", log);
        assertEquals("El endpoint debe ser correcto", "/api/almacen/inventarios", log.getEndpoint());
        assertEquals("El código HTTP debe ser 500 en error", 500, log.getCodigoHttp());
        assertNotNull("El detalle del error debe estar poblado", log.getDetalleError());
        assertTrue(
                "El detalle del error debe contener el mensaje de la excepción",
                log.getDetalleError().contains("Timeout de conexión")
        );
    }

    // ==========================================
    // TASK 7.4 — El callback onRegistroCallback se invoca exactamente una vez
    // ==========================================

    /**
     * PRESERVATION: El callback onRegistroCallback se invoca exactamente una vez
     * por llamada a execute() en el flujo exitoso.
     *
     * Validates: Requirements 3.5
     */
    @Test
    public void preservation_callbackSeInvocaExactamenteUnaVezEnExito()
            throws InterruptedException {

        // Arrange
        configurarProviderInmediato(AUDIT_INFO);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger contadorInvocaciones = new AtomicInteger(0);

        Inventario inventario = buildInventarioCompleto();

        // Act
        useCase.execute(inventario, new RegistrarInventarioUseCase.OnRegistroCallback() {
            @Override
            public void onSincronizado() {
                contadorInvocaciones.incrementAndGet();
                latch.countDown();
            }

            @Override
            public void onGuardadoLocal() {
                contadorInvocaciones.incrementAndGet();
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                contadorInvocaciones.incrementAndGet();
                latch.countDown();
            }
        });

        boolean completado = latch.await(3, TimeUnit.SECONDS);
        assertTrue("El flujo debe completar", completado);

        // Esperar un poco más para asegurarse de que no se invoca de nuevo
        Thread.sleep(300);

        // Assert: exactamente una invocación
        assertEquals(
                "El callback debe invocarse exactamente una vez por llamada a execute()",
                1,
                contadorInvocaciones.get()
        );
    }

    /**
     * PRESERVATION: El callback onRegistroCallback se invoca exactamente una vez
     * por llamada a execute() en el flujo de error (guardado local).
     *
     * Validates: Requirements 3.5
     */
    @Test
    public void preservation_callbackSeInvocaExactamenteUnaVezEnGuardadoLocal()
            throws Exception {

        // Arrange
        configurarProviderInmediato(AUDIT_INFO);
        doThrow(new RuntimeException("Sin red"))
                .when(mockInventarioRepository).enviarInventarioRemote(any());

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger contadorInvocaciones = new AtomicInteger(0);

        Inventario inventario = buildInventarioCompleto();

        // Act
        useCase.execute(inventario, new RegistrarInventarioUseCase.OnRegistroCallback() {
            @Override
            public void onSincronizado() {
                contadorInvocaciones.incrementAndGet();
                latch.countDown();
            }

            @Override
            public void onGuardadoLocal() {
                contadorInvocaciones.incrementAndGet();
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                contadorInvocaciones.incrementAndGet();
                latch.countDown();
            }
        });

        boolean completado = latch.await(3, TimeUnit.SECONDS);
        assertTrue("El flujo debe completar", completado);

        Thread.sleep(300);

        assertEquals(
                "El callback debe invocarse exactamente una vez incluso en guardado local",
                1,
                contadorInvocaciones.get()
        );
    }

    // ==========================================
    // TASK 7.5 — Sin permisos GPS, el registro continúa con coordenadas vacías
    // ==========================================

    /**
     * PRESERVATION: Cuando no hay permisos GPS, el registro continúa normalmente
     * con coordenadas vacías (no se bloquea ni lanza excepción).
     *
     * Validates: Requirements 3.1
     */
    @Test
    public void preservation_sinPermisosGPS_registroContinuaConCoordenadasVacias()
            throws Exception {

        // Arrange: provider que retorna inmediatamente con lat/lon vacíos (sin GPS)
        configurarProviderInmediato(AUDIT_INFO_SIN_GPS);
        ArgumentCaptor<Inventario> inventarioCaptor = ArgumentCaptor.forClass(Inventario.class);
        CountDownLatch latch = new CountDownLatch(1);

        Inventario inventario = buildInventarioCompleto();

        // Act
        useCase.execute(inventario, new RegistrarInventarioUseCase.OnRegistroCallback() {
            @Override
            public void onSincronizado() { latch.countDown(); }

            @Override
            public void onGuardadoLocal() { latch.countDown(); }

            @Override
            public void onError(Exception e) { latch.countDown(); }
        });

        boolean completado = latch.await(3, TimeUnit.SECONDS);

        // Assert: el flujo no se bloquea
        assertTrue("El registro debe completar aunque no haya GPS", completado);

        // El inventario se envió al servidor (no se bloqueó)
        verify(mockInventarioRepository).enviarInventarioRemote(inventarioCaptor.capture());
        Inventario enviado = inventarioCaptor.getValue();

        // auditClientInfo está presente pero con coordenadas vacías
        assertNotNull("auditClientInfo no debe ser null aunque no haya GPS", enviado.getAuditClientInfo());
        assertEquals("La latitud debe ser vacía sin GPS", "", enviado.getAuditClientInfo().getLatitud());
        assertEquals("La longitud debe ser vacía sin GPS", "", enviado.getAuditClientInfo().getLongitud());

        // Los campos de hardware siguen presentes
        assertNotNull("El dispositivo debe estar presente aunque no haya GPS",
                enviado.getAuditClientInfo().getDispositivo());
        assertNotNull("La IP debe estar presente aunque no haya GPS",
                enviado.getAuditClientInfo().getIp());
    }
}
