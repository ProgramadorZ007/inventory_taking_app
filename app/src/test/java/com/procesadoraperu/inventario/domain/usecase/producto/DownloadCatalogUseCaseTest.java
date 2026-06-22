package com.procesadoraperu.inventario.domain.usecase.producto;

import com.procesadoraperu.inventario.domain.repository.producto.IProductoRepository;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DownloadCatalogUseCase.
 * Validates: Requirements 1.1, 1.2, 2.1
 */
public class DownloadCatalogUseCaseTest {

    private IProductoRepository mockRepository;
    private DownloadCatalogUseCase useCase;

    @Before
    public void setUp() {
        mockRepository = mock(IProductoRepository.class);
        useCase = new DownloadCatalogUseCase(mockRepository);
    }

    @Test
    public void execute_withValidInputs_delegatesToRepositoryAndReturnsCount() throws Exception {
        when(mockRepository.downloadAndStoreCatalog("SUC-01", "ALM-01")).thenReturn(150);

        int result = useCase.execute("SUC-01", "ALM-01");

        assertEquals(150, result);
        verify(mockRepository).downloadAndStoreCatalog("SUC-01", "ALM-01");
    }

    @Test(expected = IllegalArgumentException.class)
    public void execute_withNullIdSucursal_throwsIllegalArgumentException() throws Exception {
        useCase.execute(null, "ALM-01");
    }

    @Test(expected = IllegalArgumentException.class)
    public void execute_withEmptyIdSucursal_throwsIllegalArgumentException() throws Exception {
        useCase.execute("", "ALM-01");
    }

    @Test(expected = IllegalArgumentException.class)
    public void execute_withBlankIdSucursal_throwsIllegalArgumentException() throws Exception {
        useCase.execute("   ", "ALM-01");
    }

    @Test(expected = IllegalArgumentException.class)
    public void execute_withNullIdAlmacen_throwsIllegalArgumentException() throws Exception {
        useCase.execute("SUC-01", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void execute_withEmptyIdAlmacen_throwsIllegalArgumentException() throws Exception {
        useCase.execute("SUC-01", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void execute_withBlankIdAlmacen_throwsIllegalArgumentException() throws Exception {
        useCase.execute("SUC-01", "   ");
    }

    @Test
    public void execute_whenRepositoryThrowsException_propagatesException() throws Exception {
        when(mockRepository.downloadAndStoreCatalog("SUC-01", "ALM-01"))
                .thenThrow(new Exception("Error de red"));

        try {
            useCase.execute("SUC-01", "ALM-01");
            fail("Expected Exception to be thrown");
        } catch (Exception e) {
            assertEquals("Error de red", e.getMessage());
        }
    }
}
