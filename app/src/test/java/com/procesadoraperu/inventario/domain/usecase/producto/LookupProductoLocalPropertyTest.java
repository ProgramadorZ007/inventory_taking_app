package com.procesadoraperu.inventario.domain.usecase.producto;

import com.procesadoraperu.inventario.domain.repository.producto.IProductoRepository;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.StringLength;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Feature: offline-product-catalog, Property 4: Non-existent ID lookup returns not-found
 *
 * Validates: Requirements 3.3
 *
 * For any product ID string not present in the repository, the use case
 * throws an exception indicating the product was not found.
 */
public class LookupProductoLocalPropertyTest {

    /**
     * Property 4: Non-existent ID lookup returns not-found
     *
     * **Validates: Requirements 3.3**
     *
     * For any arbitrary non-empty product ID (up to 50 characters), when the
     * repository returns null (product not in store), the use case throws an exception.
     */
    @Property(tries = 100)
    void lookupNonExistentIdAlwaysThrows(
            @ForAll @StringLength(min = 1, max = 50) String idProducto
    ) {
        IProductoRepository mockRepository = mock(IProductoRepository.class);
        when(mockRepository.getProductoLocal(idProducto.trim())).thenReturn(null);

        LookupProductoLocalUseCase useCase = new LookupProductoLocalUseCase(mockRepository);

        assertThrows(Exception.class, () -> useCase.execute(idProducto));
    }
}
