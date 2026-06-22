package com.procesadoraperu.inventario.domain.usecase.inventario;

import android.content.Context;

import com.procesadoraperu.inventario.domain.model.inventario.AuditClientInfo;
import com.procesadoraperu.inventario.domain.model.inventario.Inventario;
import com.procesadoraperu.inventario.domain.provider.IAuditClientInfoProvider;
import com.procesadoraperu.inventario.domain.repository.inventario.IInventarioRepository;
import com.procesadoraperu.inventario.domain.repository.log.ILogRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests de Fix Checking.
 *
 * Verifican que el RegistrarInventarioUseCase corregido funciona correctamente:
 * auditClientInfo está poblado en el request, los callbacks se invocan correctamente,
 * y el flujo offline también funciona con auditClientInfo poblado.
 *
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4
 */
public class RegistrarInventarioUseCaseFixTest {

    private IInventarioRepository mockInventarioRepository;
    private ILogRepository mockLogRepository;
    private IAuditClientInfoProvider mockAuditProvider;
    private RegistrarInventarioUseCase useCase;

    private static final AuditClientInfo AUDIT_INFO_CON_GPS = new AuditClientInfo(
            "Samsung Galaxy S21", "192.168.1.100", "android-device", "Dalvik/2.1.0", "-12.0464", "-77.0428"
    );

    private static final AuditClientInfo AUDIT_INFO_SIN_GPS = new AuditClientInfo(
            "Samsung Galaxy S21", "192.168.1.100", "android-device", "Dalvik/2.1.0", "", ""
    );

    @Before
    public void setUp() {
        mockInventarioRepository = mock(IInventarioRepository.class);
        mockLogRepository = mock(ILogRepository.class);
        mockAuditProvider = mock(IAuditClientInfoProvider.class);
        Context mockContext = mock(Context.class);
        org.mockito.Mockito.when(mockContext.getApplicationContext()).thenReturn(mockContext);
        useCase = new RegistrarInventarioUseCase(
                mockInventarioRepository, mockLogRepository, mockAuditProvider, mockContext
        );
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

    // ==========================================
    // TASK 6.2 — Callback retardado 200ms: onSincronizado() se invoca y auditClientInfo está poblado
    // ==========================================

    /**
     * FIX VERIFICADO: Con un provider que retarda el callback 200ms,
     * el use case corregido espera el callback antes de enviar al servidor.
     * onSincronizado() se invoca y auditClientInfo está poblado en el request.
     *
     * Validates: Requirements 2.1, 2.2, 2.3
     */
    @Test
    public void fix_conCallbackRetardado200ms_onSincronizadoSeInvocaYAuditClientInfoEstaPoblado()
            throws Exception {

        // Arrange: provider que retarda el callback 200ms
        doAnswer(invocation -> {
            IAuditClientInfoProvider.OnAuditInfoCallback callback =
                    invocation.getArgument(0);
            new Thread(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {}
                callback.onSuccess(AUDIT_INFO_CON_GPS);
            }).start();
            return null;
        }).when(mockAuditProvider).getAuditInfo(any());

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean sincronizadoInvocado = new AtomicBoolean(false);
        ArgumentCaptor<Inventario> inventarioCaptor = ArgumentCaptor.forClass(Inventario.class);

        Inventario inventario = buildInventario();

        // Act
        useCase.execute(inventario, new RegistrarInventarioUseCase.OnRegistroCallback() {
            @Override
            public void onSincronizado() {
                sincronizadoInvocado.set(true);
                latch.countDown();
            }

            @Override
            public void onGuardadoLocal() {
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                latch.countDown();
            }
        });

        // Esperar a que el flujo complete (máximo 3 segundos)
        boolean completado = latch.await(3, TimeUnit.SECONDS);

        // Assert
        assertTrue("El flujo debe completar dentro del timeout", completado);
        assertTrue("onSincronizado() debe haberse invocado", sincronizadoInvocado.get());

        verify(mockInventarioRepository).enviarInventarioRemote(inventarioCaptor.capture());
        Inventario inventarioEnviado = inventarioCaptor.getValue();

        assertNotNull(
                "FIX VERIFICADO: auditClientInfo debe estar poblado en el request enviado",
                inventarioEnviado.getAuditClientInfo()
        );
        assertNotNull(
                "El campo dispositivo debe estar poblado",
                inventarioEnviado.getAuditClientInfo().getDispositivo()
        );
        assertNotNull(
                "El campo ip debe estar poblado",
                inventarioEnviado.getAuditClientInfo().getIp()
        );
        assertEquals(
                "El dispositivo debe coincidir con el mock",
                "Samsung Galaxy S21",
                inventarioEnviado.getAuditClientInfo().getDispositivo()
        );
    }

    // ==========================================
    // TASK 6.3 — Mock de red que lanza excepción: onGuardadoLocal() se invoca con auditClientInfo poblado
    // ==========================================

    /**
     * FIX VERIFICADO: Cuando el envío al servidor falla (excepción de red),
     * onGuardadoLocal() se invoca y el inventario se guarda localmente
     * con auditClientInfo correctamente poblado.
     *
     * Validates: Requirements 2.1, 2.2, 3.2
     */
    @Test
    public void fix_conExcepcionDeRed_onGuardadoLocalSeInvocaYAuditClientInfoEstaPoblado()
            throws Exception {

        // Arrange: provider que retorna inmediatamente
        doAnswer(invocation -> {
            IAuditClientInfoProvider.OnAuditInfoCallback callback =
                    invocation.getArgument(0);
            callback.onSuccess(AUDIT_INFO_CON_GPS);
            return null;
        }).when(mockAuditProvider).getAuditInfo(any());

        // Red que lanza excepción
        doThrow(new RuntimeException("Sin conexión a internet"))
                .when(mockInventarioRepository).enviarInventarioRemote(any());

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean guardadoLocalInvocado = new AtomicBoolean(false);
        ArgumentCaptor<Inventario> inventarioCaptor = ArgumentCaptor.forClass(Inventario.class);

        Inventario inventario = buildInventario();

        // Act
        useCase.execute(inventario, new RegistrarInventarioUseCase.OnRegistroCallback() {
            @Override
            public void onSincronizado() {
                latch.countDown();
            }

            @Override
            public void onGuardadoLocal() {
                guardadoLocalInvocado.set(true);
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                latch.countDown();
            }
        });

        boolean completado = latch.await(3, TimeUnit.SECONDS);

        // Assert
        assertTrue("El flujo debe completar dentro del timeout", completado);
        assertTrue("onGuardadoLocal() debe haberse invocado", guardadoLocalInvocado.get());

        verify(mockInventarioRepository).saveInventarioLocal(inventarioCaptor.capture());
        Inventario inventarioGuardado = inventarioCaptor.getValue();

        assertNotNull(
                "FIX VERIFICADO: auditClientInfo debe estar poblado en el inventario guardado localmente",
                inventarioGuardado.getAuditClientInfo()
        );
        assertEquals(
                "El estado debe ser PENDIENTE al guardar localmente",
                "PENDIENTE",
                inventarioGuardado.getEstadoSincronizacion()
        );
    }

    // ==========================================
    // TASK 6.4 — getAuditInfo() retorna inmediatamente sin GPS: auditClientInfo tiene campos de hardware
    // ==========================================

    /**
     * FIX VERIFICADO: Cuando getAuditInfo() retorna inmediatamente sin GPS
     * (lat/lon vacíos), auditClientInfo tiene los campos de hardware correctos
     * y el registro no se bloquea.
     *
     * Validates: Requirements 2.4, 3.1
     */
    @Test
    public void fix_sinGPS_auditClientInfoTieneCamposDeHardwareCorrectosYRegistroContinua()
            throws Exception {

        // Arrange: provider que retorna inmediatamente con lat/lon vacíos (sin GPS)
        doAnswer(invocation -> {
            IAuditClientInfoProvider.OnAuditInfoCallback callback =
                    invocation.getArgument(0);
            callback.onSuccess(AUDIT_INFO_SIN_GPS);
            return null;
        }).when(mockAuditProvider).getAuditInfo(any());

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean callbackInvocado = new AtomicBoolean(false);
        ArgumentCaptor<Inventario> inventarioCaptor = ArgumentCaptor.forClass(Inventario.class);

        Inventario inventario = buildInventario();

        // Act
        useCase.execute(inventario, new RegistrarInventarioUseCase.OnRegistroCallback() {
            @Override
            public void onSincronizado() {
                callbackInvocado.set(true);
                latch.countDown();
            }

            @Override
            public void onGuardadoLocal() {
                callbackInvocado.set(true);
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                latch.countDown();
            }
        });

        boolean completado = latch.await(3, TimeUnit.SECONDS);

        // Assert
        assertTrue("El flujo debe completar sin bloquearse aunque no haya GPS", completado);
        assertTrue("El callback debe haberse invocado", callbackInvocado.get());

        verify(mockInventarioRepository).enviarInventarioRemote(inventarioCaptor.capture());
        Inventario inventarioEnviado = inventarioCaptor.getValue();

        assertNotNull(
                "auditClientInfo no debe ser null aunque no haya GPS",
                inventarioEnviado.getAuditClientInfo()
        );
        assertEquals(
                "El campo dispositivo debe estar poblado con datos de hardware",
                "Samsung Galaxy S21",
                inventarioEnviado.getAuditClientInfo().getDispositivo()
        );
        assertEquals(
                "El campo ip debe estar poblado con datos de hardware",
                "192.168.1.100",
                inventarioEnviado.getAuditClientInfo().getIp()
        );
        assertEquals(
                "La latitud debe ser vacía cuando no hay GPS",
                "",
                inventarioEnviado.getAuditClientInfo().getLatitud()
        );
        assertEquals(
                "La longitud debe ser vacía cuando no hay GPS",
                "",
                inventarioEnviado.getAuditClientInfo().getLongitud()
        );
    }
}
