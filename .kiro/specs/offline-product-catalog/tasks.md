# Implementation Plan: Offline Product Catalog

## Overview

This plan implements the offline product catalog feature for the ProcesadoraPeru inventory app. The implementation follows the existing MVVM architecture with Room, Retrofit, and LiveData. Tasks progress from data layer changes through domain use cases to presentation layer integration, ensuring each step builds on the previous one with no orphaned code.

## Tasks

- [x] 1. Extend data layer (DAO + Repository interface + implementation)
  - [x] 1.1 Add `getProductCount()` query to `ProductoDao` and extend `IProductoRepository`
    - Add `@Query("SELECT COUNT(*) FROM Producto") int getProductCount();` to `ProductoDao.java`
    - Add new methods to `IProductoRepository.java`: `int downloadAndStoreCatalog(String idSucursal, String idAlmacen) throws Exception`, `Producto getProductoLocal(String idProducto)`, `void clearLocalCatalog()`, `int getLocalProductCount()`
    - _Requirements: 2.1, 3.1, 4.4, 5.1_

  - [x] 1.2 Implement repository methods in `ProductoRepositoryImpl`
    - Implement `downloadAndStoreCatalog()`: build `ProductoStockRequest` with null `idProducto`, call API, map response to entities, call `productoDao.refreshCatalogo(entities)`, return count
    - Implement `getProductoLocal()`: call `productoDao.getProducto(idProducto)`, map entity to domain model, return null if not found
    - Implement `clearLocalCatalog()`: call `productoDao.deleteAll()`
    - Implement `getLocalProductCount()`: call `productoDao.getProductCount()`
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 3.1, 3.5, 4.4, 5.1_

- [x] 2. Create domain use cases
  - [x] 2.1 Create `DownloadCatalogUseCase`
    - Create file at `domain/usecase/producto/DownloadCatalogUseCase.java`
    - Constructor receives `IProductoRepository`
    - `execute(String idSucursal, String idAlmacen)` validates inputs are non-null/non-empty, delegates to `productoRepository.downloadAndStoreCatalog()`, returns product count
    - Throw `IllegalArgumentException` if idSucursal or idAlmacen is null/empty
    - _Requirements: 1.1, 1.2, 2.1_

  - [x] 2.2 Create `LookupProductoLocalUseCase`
    - Create file at `domain/usecase/producto/LookupProductoLocalUseCase.java`
    - Constructor receives `IProductoRepository`
    - `execute(String idProducto)` validates input (null/empty check, truncate to 50 chars), calls `productoRepository.getProductoLocal(idProducto)`, throws if not found
    - Throw `IllegalArgumentException` if idProducto is null or empty with message "El código escaneado no contiene un identificador válido"
    - Throw appropriate exception if product not found in local catalog
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [x] 2.3 Create `ClearCatalogUseCase`
    - Create file at `domain/usecase/producto/ClearCatalogUseCase.java`
    - Constructor receives `IProductoRepository`
    - `execute()` delegates to `productoRepository.clearLocalCatalog()`
    - _Requirements: 4.4_

  - [x] 2.4 Create `GetCatalogCountUseCase`
    - Create file at `domain/usecase/producto/GetCatalogCountUseCase.java`
    - Constructor receives `IProductoRepository`
    - `execute()` delegates to `productoRepository.getLocalProductCount()`, returns int
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 2.5 Write property test: Catalog request omits idProducto (Property 1)
    - **Property 1: Catalog request omits idProducto**
    - **Validates: Requirements 1.1**
    - Add jqwik dependency `testImplementation("net.jqwik:jqwik:1.8.4")` to `build.gradle.kts`
    - Create `app/src/test/java/com/procesadoraperu/inventario/domain/usecase/producto/DownloadCatalogPropertyTest.java`
    - For any valid idSucursal and idAlmacen strings, verify the ProductoStockRequest constructed has null idProducto
    - Use `@Property(tries = 100)` with `@ForAll @StringLength(min=1, max=20)` providers

  - [ ] 2.6 Write property test: Non-existent ID lookup returns not-found (Property 4)
    - **Property 4: Non-existent ID lookup returns not-found**
    - **Validates: Requirements 3.3**
    - Create `app/src/test/java/com/procesadoraperu/inventario/domain/usecase/producto/LookupProductoLocalPropertyTest.java`
    - For any product ID string not present in the repository, verify the use case throws an exception
    - Mock `IProductoRepository.getProductoLocal()` to return null for arbitrary IDs

- [x] 3. Create `DownloadResult` model class
  - [x] 3.1 Create `DownloadResult` in the domain model package
    - Create file at `domain/model/DownloadResult.java`
    - Define enum `Status { SUCCESS, ERROR, EMPTY }`
    - Fields: `Status status`, `int productCount`, `String errorMessage`
    - Static factory methods: `success(int count)`, `error(String message)`, `empty()`
    - Private constructor, getters
    - _Requirements: 1.4, 1.5, 1.6_

- [x] 4. Checkpoint - Ensure data and domain layers compile
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Update `SelectionViewModel` for catalog download
  - [x] 5.1 Add download state management to `SelectionViewModel`
    - Add `MutableLiveData<Boolean> isDownloading` field
    - Add `MutableLiveData<DownloadResult> downloadResult` field
    - Inject `DownloadCatalogUseCase` via constructor
    - Implement `downloadCatalogAndNavigate(String idSucursal, String idAlmacen)`: set isDownloading=true, call use case on background thread, post DownloadResult on completion, set isDownloading=false
    - Handle exceptions: catch network/server errors and post `DownloadResult.error(message)`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

  - [ ]* 5.2 Write unit tests for `SelectionViewModel` download flow
    - Create `app/src/test/java/com/procesadoraperu/inventario/presentation/selection/SelectionViewModelTest.java`
    - Test: successful download transitions isDownloading true→false and posts SUCCESS result
    - Test: network failure posts ERROR result with message
    - Test: empty catalog posts EMPTY result
    - _Requirements: 1.3, 1.4, 1.5, 1.6_

- [x] 6. Update `HomeViewModel` for catalog status and clear
  - [x] 6.1 Add catalog count and clear logic to `HomeViewModel`
    - Inject `GetCatalogCountUseCase` and `ClearCatalogUseCase` via constructor
    - Add `MutableLiveData<Integer> catalogCount` field
    - Add `MutableLiveData<Boolean> clearSuccess` field
    - Implement `loadCatalogCount()`: call use case, post result to catalogCount
    - Implement `clearCatalog()`: call use case on background thread, post true on success / false on failure, reload count after clear
    - _Requirements: 4.4, 5.1, 5.2, 5.3_

  - [ ]* 6.2 Write unit tests for `HomeViewModel` catalog logic
    - Create `app/src/test/java/com/procesadoraperu/inventario/presentation/home/HomeViewModelTest.java`
    - Test: loadCatalogCount posts correct count
    - Test: clearCatalog posts true and count becomes 0
    - Test: clearCatalog failure posts false
    - _Requirements: 4.4, 5.1_

- [x] 7. Update `ViewModelFactory` for new dependencies
  - [x] 7.1 Wire use cases into `ViewModelFactory`
    - Update `ViewModelFactory.java` to instantiate `DownloadCatalogUseCase`, `LookupProductoLocalUseCase`, `ClearCatalogUseCase`, `GetCatalogCountUseCase`
    - Pass use cases to `SelectionViewModel` and `HomeViewModel` constructors
    - Ensure `ProductoRepositoryImpl` is available as singleton or created with correct dependencies
    - _Requirements: 1.1, 3.1, 4.4, 5.1_

- [x] 8. Checkpoint - Ensure ViewModels compile and unit tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Update `AlmacenActivity` UI for download flow
  - [x] 9.1 Implement catalog download UI in `AlmacenActivity`
    - Add a `ProgressBar` overlay (or loading layout) shown during download
    - After almacen selection, call `viewModel.downloadCatalogAndNavigate(idSucursal, idAlmacen)` instead of navigating directly
    - Observe `isDownloading` LiveData: show/hide progress overlay, disable almacen list during download
    - Observe `downloadResult` LiveData:
      - SUCCESS → navigate to HomeActivity
      - ERROR → show AlertDialog with error message, "Reintentar" button, and "Continuar sin catálogo" button
      - EMPTY → show AlertDialog with "No se encontraron productos" message, option to continue or select different almacen
    - _Requirements: 1.3, 1.4, 1.5, 1.6_

- [x] 10. Update `HomeActivity` UI for catalog status
  - [x] 10.1 Implement catalog status display and clear button in `HomeActivity`
    - Add a `TextView` for catalog count (e.g., "150 productos disponibles offline" or "Sin catálogo offline")
    - Add a "Limpiar Catálogo" button
    - Observe `catalogCount` LiveData: update TextView text based on count (0 → "Sin catálogo offline", >0 → "{count} productos disponibles offline")
    - On clear button click: show confirmation AlertDialog ("Se eliminarán todos los datos del catálogo offline", "Confirmar" / "Cancelar")
    - On confirm: call `viewModel.clearCatalog()`
    - Observe `clearSuccess`: show Toast on success ("Catálogo limpiado exitosamente") or error ("No se pudo limpiar el catálogo. Intenta de nuevo.")
    - Call `viewModel.loadCatalogCount()` in `onResume()` to refresh count
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 5.1, 5.2, 5.3_

- [x] 11. Integrate `LookupProductoLocalUseCase` into inventory scan flow
  - [x] 11.1 Wire local lookup into the existing QR scan handling
    - In the ViewModel/Activity that handles QR scan results (TakeInventoryViewModel or equivalent), inject `LookupProductoLocalUseCase`
    - When a QR code is scanned, call `LookupProductoLocalUseCase.execute(scannedId)` first for local lookup
    - If local lookup returns a product, use it without making a network call
    - If local lookup throws (not found), fall back to the existing online `ConsultarStockProductoUseCase` behavior
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 12. Checkpoint - Ensure full app compiles and UI works
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 13. Property-based tests for data layer (Room)
  - [ ]* 13.1 Write property test: Catalog refresh replaces all data (Property 2)
    - **Property 2: Catalog refresh replaces all data**
    - **Validates: Requirements 2.1, 2.3, 2.4**
    - Create `app/src/androidTest/java/com/procesadoraperu/inventario/data/local/dao/ProductoDaoPropertyTest.java`
    - Use in-memory Room database
    - For any two arbitrary lists of ProductoEntity A and B, after inserting A then calling `refreshCatalogo(B)`, the DB SHALL contain exactly the elements of B
    - Use jqwik `@Property(tries = 100)` with custom `@Provide` methods for ProductoEntity lists

  - [ ]* 13.2 Write property test: Store-then-lookup round trip (Property 3)
    - **Property 3: Store-then-lookup round trip**
    - **Validates: Requirements 3.1, 3.2**
    - For any valid ProductoEntity stored via `refreshCatalogo`, querying by that product's idProducto returns a record with equivalent field values
    - Verify all mapped fields: descripcion, stock, disponible, idMedida, nombreComercial, idGrupo, grupoDsc, idSubGrupo, subgrupoDsc, idUbicacion, tipoproducto, propiedad, idCultivo, cultivo, idVariedad, variedad, estado

  - [ ]* 13.3 Write property test: Confirmed clear empties the store (Property 6)
    - **Property 6: Confirmed clear empties the store**
    - **Validates: Requirements 4.4**
    - For any list of N products stored via `refreshCatalogo`, after calling `deleteAll()`, `getProductCount()` SHALL return 0

  - [ ]* 13.4 Write property test: Product count reflects stored catalog size (Property 7)
    - **Property 7: Product count reflects stored catalog size**
    - **Validates: Requirements 5.1, 5.2, 5.3**
    - For any list of N products stored via `refreshCatalogo`, `getProductCount()` SHALL return exactly N

- [ ] 14. Unit tests for use cases
  - [ ]* 14.1 Write unit tests for `DownloadCatalogUseCase`
    - Create `app/src/test/java/com/procesadoraperu/inventario/domain/usecase/producto/DownloadCatalogUseCaseTest.java`
    - Test: valid inputs delegate to repository and return count
    - Test: null idSucursal throws IllegalArgumentException
    - Test: empty idAlmacen throws IllegalArgumentException
    - Test: repository exception propagates
    - _Requirements: 1.1, 1.2, 1.5_

  - [ ]* 14.2 Write unit tests for `LookupProductoLocalUseCase`
    - Create `app/src/test/java/com/procesadoraperu/inventario/domain/usecase/producto/LookupProductoLocalUseCaseTest.java`
    - Test: valid ID returns product from repository
    - Test: null input throws IllegalArgumentException with expected message
    - Test: empty string throws IllegalArgumentException
    - Test: product not found throws exception
    - Test: input > 50 chars is truncated before lookup
    - _Requirements: 3.1, 3.3, 3.4_

  - [ ]* 14.3 Write unit tests for `ClearCatalogUseCase` and `GetCatalogCountUseCase`
    - Create test files for both use cases
    - Test: ClearCatalogUseCase delegates to repository.clearLocalCatalog()
    - Test: GetCatalogCountUseCase returns repository.getLocalProductCount() value
    - _Requirements: 4.4, 5.1_

- [x] 15. Final checkpoint - All tests pass, feature complete
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document using jqwik
- Unit tests validate specific examples and edge cases using JUnit + Mockito
- The existing `ProductoEntity` and `ProductoDao.refreshCatalogo()` are reused — no schema migration required
- jqwik dependency must be added in task 2.5 before any property tests can run
- Property 5 (cancel clear is a no-op) is implicitly verified by UI behavior (dialog cancellation never calls the DAO)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "3.1"] },
    { "id": 1, "tasks": ["1.2"] },
    { "id": 2, "tasks": ["2.1", "2.2", "2.3", "2.4"] },
    { "id": 3, "tasks": ["2.5", "2.6", "5.1", "6.1", "7.1"] },
    { "id": 4, "tasks": ["5.2", "6.2", "9.1", "10.1", "11.1"] },
    { "id": 5, "tasks": ["13.1", "13.2", "13.3", "13.4", "14.1", "14.2", "14.3"] }
  ]
}
```
