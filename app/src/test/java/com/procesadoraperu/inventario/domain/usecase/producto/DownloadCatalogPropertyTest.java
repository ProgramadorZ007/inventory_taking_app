package com.procesadoraperu.inventario.domain.usecase.producto;

import com.procesadoraperu.inventario.data.remote.request.ProductoStockRequest;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.StringLength;

import java.lang.reflect.Field;

import static org.junit.Assert.assertNull;

/**
 * Feature: offline-product-catalog, Property 1: Catalog request omits idProducto
 *
 * Validates: Requirements 1.1
 *
 * For any valid idSucursal and idAlmacen strings, the ProductoStockRequest
 * constructed for catalog download always has null idProducto.
 */
public class DownloadCatalogPropertyTest {

    /**
     * Property 1: Catalog request omits idProducto
     *
     * **Validates: Requirements 1.1**
     *
     * For any valid idSucursal and idAlmacen, constructing a catalog request
     * (with null as the third argument) always results in idProducto being null.
     */
    @Property(tries = 100)
    void catalogRequestAlwaysOmitsIdProducto(
            @ForAll @StringLength(min = 1, max = 20) String idSucursal,
            @ForAll @StringLength(min = 1, max = 20) String idAlmacen
    ) throws Exception {
        // Construct the request exactly as ProductoRepositoryImpl.downloadAndStoreCatalog() does
        ProductoStockRequest request = new ProductoStockRequest(idSucursal, idAlmacen, null);

        // Use reflection to access the private idProducto field
        Field idProductoField = ProductoStockRequest.class.getDeclaredField("idProducto");
        idProductoField.setAccessible(true);
        Object idProductoValue = idProductoField.get(request);

        assertNull("idProducto must be null for catalog requests", idProductoValue);
    }
}
