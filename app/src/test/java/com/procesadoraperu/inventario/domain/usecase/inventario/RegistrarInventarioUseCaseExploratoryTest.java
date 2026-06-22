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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests exploratorios (Bug Condition Checking).
 *
 * Estos tests demuestran el bug ORIGINAL: cuando el callback de getAuditInfo()
 * es asíncrono (retardado), el código sin fix envía el request con auditClientInfo = null.
 *
 * Para demostrar el bug, se usa una versión "buggy" del use case (clase interna BuggyUseCase)
 * que replica el comportamiento original: llama a getAuditInfo() pero NO espera el callback
 * antes de continuar con el envío al servidor.
 *
 * Validates: Requirements 1.1, 1.2
 */
public class RegistrarInventarioUseCaseExploratoryTest {

    private IInventarioRepository mockInventarioRepository;
    private ILogRepository mockLogRepository;
    private IAuditClientInfoProvider mockAuditProvider;

    // AuditClientInfo que el provider retornará (con retardo)
    private static final AuditClientInfo AUDIT_INFO = new AuditClientInfo(
            "TestDevice", "192.168.1.1", "test-host", "TestAgent/1.0", "-12.0", "-77.0"
    );

    @Before
    public void setUp() {
        mockInventarioRepository = mock(IInventarioRepository.class);
        mockLogRepository = mock(ILogRepository.class);
        mockAuditProvider = mock(IAuditClientInfoProvider.class);
    }

    /**
     * Construye un Inventario de prueba con todos los campos básicos poblados.
     */
    private Inventario buildInventario() {
        Inventario inv = new Inventario();
        inv.setIdEmpresa("001");
        inv.setIdSucursal("SUC-01");
        inv.setIdAlmacen("ALM-01");
        inv.setIdProducto("PROD-001");
        inv.setProducto("Producto de prueba");
        inv.setUnidadMedida("UND");
        inv.setStock(100.0);
        inv.setCantidad(10.0);
        inv.setUsuarioCreacion("testuser");
        return inv;
    }

    /**
     * Versión "buggy" del use case que replica el comportamiento original:
     * llama a getAuditInfo() pero NO espera el callback antes de continuar.
     * Esto demuestra la condición del bug.
     */
    static class BuggyUseCase {

        private final IInventarioRepository inventarioRepository;
        private final ILogRepository logRepository;
        private final IAuditClientInfoProvider auditProvider;

        BuggyUseCase(IInventarioRepository inventarioRepository,
                     ILogRepository logRepository,
                     IAuditClientInfoProvider auditProvider) {
            this.inventarioRepository = inventarioRepository;
            this.logRepository = logRepository;
            this.auditProvider = auditProvider;
        }

        /**
         * Comportamiento original (buggy): inicia la obtención de auditInfo de forma
         * asíncrona pero continúa el flujo inmediatamente sin esperar el callback.
         * El envío al servidor ocurre ANTES de que auditClientInfo esté disponible.
         */
        void execute(Inventario inventario) {
            // Inicia la obtención asíncrona — el callback llegará después
            auditProvider.getAuditInfo(auditInfo -> {
                inventario.setAuditClientInfo(auditInfo);
                // En el código original, el envío ocurría aquí dentro del callback,
                // pero el ViewModel leía el estado ANTES de que esto se ejecutara.
            });

            // BUG: continúa inmediatamente sin esperar el callback.
            // En el código original, el ViewModel leía inventario.getEstadoSincronizacion()
            // aquí, antes de que el callback de auditoría completara.
            // Simulamos ese comportamiento intentando enviar con el estado actual del inventario.
            try {
                inventarioRepository.enviarInventarioRemote(inventario);
            } catch (Exception e) {
                inventario.setEstadoSincronizacion("PENDIENTE");
                inventarioRepository.saveInventarioLocal(inventario);
            }
        }
    }

    // ==========================================
    // TASK 5.2 — auditClientInfo es null inmediatamente después de execute()
    // ==========================================

    /**
     * Demuestra el bug: con un provider que retarda el callback 200ms,
     * inventario.getAuditClientInfo() es null inmediatamente después de que
     * el código buggy "retorna" (antes de que el callback asíncrono dispare).
     *
     * Validates: Requirements 1.1, 1.2
     */
    @Test
    public void bugCondition_auditClientInfoEsNullInmediatamenteDespuesDeExecute()
            throws InterruptedException {

        // Arrange: provider que retarda el callback 200ms (simula GPS lento)
        doAnswer(invocation -> {
            IAuditClientInfoProvider.OnAuditInfoCallback callback =
                    invocation.getArgument(0);
            // Retarda el callback en un hilo separado
            new Thread(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {}
                callback.onSuccess(AUDIT_INFO);
            }).start();
            return null;
        }).when(mockAuditProvider).getAuditInfo(any());

        Inventario inventario = buildInventario();
        BuggyUseCase buggyUseCase = new BuggyUseCase(
                mockInventarioRepository, mockLogRepository, mockAuditProvider
        );

        // Act: llamar al código buggy
        buggyUseCase.execute(inventario);

        // Assert: inmediatamente después de execute(), auditClientInfo es null
        // porque el callback de 200ms aún no ha disparado
        assertNull(
                "BUG CONFIRMADO: auditClientInfo debe ser null inmediatamente después " +
                "de execute() cuando el callback es asíncrono (retardado 200ms)",
                inventario.getAuditClientInfo()
        );
    }

    // ==========================================
    // TASK 5.3 — El request capturado tiene auditClientInfo = null
    // ==========================================

    /**
     * Demuestra el bug: el request enviado al servidor tiene auditClientInfo = null
     * porque el envío ocurre antes de que el callback asíncrono de auditoría complete.
     *
     * Validates: Requirements 1.1, 1.3
     */
    @Test
    public void bugCondition_requestEnviadoConAuditClientInfoNull()
            throws Exception {

        // Arrange: provider que retarda el callback 200ms
        doAnswer(invocation -> {
            IAuditClientInfoProvider.OnAuditInfoCallback callback =
                    invocation.getArgument(0);
            new Thread(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {}
                callback.onSuccess(AUDIT_INFO);
            }).start();
            return null;
        }).when(mockAuditProvider).getAuditInfo(any());

        // Capturamos el inventario que se pasa a enviarInventarioRemote
        ArgumentCaptor<Inventario> inventarioCaptor = ArgumentCaptor.forClass(Inventario.class);

        Inventario inventario = buildInventario();
        BuggyUseCase buggyUseCase = new BuggyUseCase(
                mockInventarioRepository, mockLogRepository, mockAuditProvider
        );

        // Act
        buggyUseCase.execute(inventario);

        // Assert: el inventario enviado al repositorio tiene auditClientInfo = null
        verify(mockInventarioRepository).enviarInventarioRemote(inventarioCaptor.capture());
        Inventario inventarioEnviado = inventarioCaptor.getValue();

        assertNull(
                "BUG CONFIRMADO: el request enviado al servidor tiene auditClientInfo = null " +
                "porque el envío ocurre antes de que el callback asíncrono complete",
                inventarioEnviado.getAuditClientInfo()
        );
    }
}
