# Requirements Document

## Introduction

The Offline Product Catalog feature enables the ProcesadoraPeru inventory app to function in warehouses without internet connectivity. The system downloads all products for a given sucursal/almacen combination while the operator has internet access (during selection), stores them locally in SQLite, and performs product lookups offline when scanning QR codes in the warehouse.

## Glossary

- **Catalog_Downloader**: The component responsible for fetching all products from the remote API for a specific sucursal/almacen combination and persisting them locally.
- **Local_Product_Store**: The SQLite database (Room) layer that stores downloaded product data for offline access.
- **Product_Lookup_Service**: The component that resolves product information from the local SQLite store when a QR code is scanned.
- **Operator**: The warehouse worker who uses the app to scan products and take inventory.
- **Sucursal**: A branch location in the organization hierarchy.
- **Almacen**: A warehouse belonging to a sucursal.
- **Product_Catalog**: The complete set of products with their stock information for a given sucursal/almacen combination.
- **ProductoStockRequest**: The API request body sent to `/api/nisira/producto-stock`. When `idProducto` is omitted, the API returns all products for that sucursal/almacen.

## Requirements

### Requirement 1: Download Product Catalog After Almacen Selection

**User Story:** As an Operator, I want the app to automatically download all products for my selected sucursal and almacen, so that I can work offline in the warehouse.

#### Acceptance Criteria

1. WHEN the Operator selects an almacen in AlmacenActivity, THE Catalog_Downloader SHALL send a request to `/api/nisira/producto-stock` with body `{"idSucursal": "<selected>", "idAlmacen": "<selected>"}` (without idProducto) to retrieve all products for that combination.
2. WHEN the API returns a response with `success=true` and a non-empty product list, THE Catalog_Downloader SHALL store all returned products in the Local_Product_Store.
3. WHILE the download is in progress, THE Catalog_Downloader SHALL display a progress indicator to the Operator and disable further almacen selection until the download completes or fails.
4. WHEN the download completes successfully and at least one product has been stored, THE Catalog_Downloader SHALL navigate the Operator to HomeActivity.
5. IF the API request fails due to a network error, a response timeout exceeding 30 seconds, or a server error (success=false), THEN THE Catalog_Downloader SHALL display an error message indicating the failure reason and present a retry button that allows the Operator to re-attempt the download, while also providing a separate option to navigate to HomeActivity without downloading.
6. IF the API returns `success=true` but the product list is empty, THEN THE Catalog_Downloader SHALL display a message indicating that no products were found for the selected sucursal/almacen combination and allow the Operator to navigate to HomeActivity or select a different almacen.

### Requirement 2: Replace Local Data on Each Download

**User Story:** As an Operator, I want the local product data to be refreshed each time I select an almacen, so that I always have the most current catalog without accumulating stale data.

#### Acceptance Criteria

1. WHEN the Catalog_Downloader receives new product data from the API, THE Local_Product_Store SHALL delete all previously stored products before inserting the new data.
2. THE Local_Product_Store SHALL execute the delete and insert operations within a single database transaction so that if any operation fails, all changes are rolled back and the previously stored products remain intact.
3. WHEN the replacement transaction completes successfully, THE Local_Product_Store SHALL contain exactly the products returned by the latest API response with a row count equal to the number of products in that response.
4. IF the API returns a successful response containing zero products, THEN THE Local_Product_Store SHALL delete all previously stored products, resulting in an empty local catalog.

### Requirement 3: Offline Product Lookup by QR Code

**User Story:** As an Operator, I want to scan a product QR code and retrieve product information from local storage, so that I can take inventory without internet access.

#### Acceptance Criteria

1. WHEN the Operator scans a QR code containing a product ID (a non-empty text string of up to 50 characters), THE Product_Lookup_Service SHALL query the Local_Product_Store for a product whose `idProducto` matches that string exactly.
2. WHEN a matching product is found in the Local_Product_Store, THE Product_Lookup_Service SHALL return the product data including: descripcion, stock, disponible, idMedida, nombreComercial, idGrupo, grupoDsc, idSubGrupo, subgrupoDsc, idUbicacion, tipoproducto, propiedad, idCultivo, cultivo, idVariedad, variedad, and estado.
3. IF no matching product is found in the Local_Product_Store, THEN THE Product_Lookup_Service SHALL return an error indicating the product was not found in the offline catalog.
4. IF the scanned QR code content is empty or null, THEN THE Product_Lookup_Service SHALL return an error indicating that the scanned code does not contain a valid product identifier.
5. THE Product_Lookup_Service SHALL perform the lookup without making any network request and SHALL return the result within 1 second.

### Requirement 4: Manual Catalog Clear

**User Story:** As an Operator, I want a button to manually delete all stored product data, so that I can free storage or reset the local catalog when needed.

#### Acceptance Criteria

1. THE app SHALL provide a button labeled with clear intent (e.g., "Limpiar Catálogo") on the home screen to clear the offline product catalog.
2. WHEN the Operator presses the clear catalog button, THE app SHALL display a confirmation dialog that includes a message indicating all offline product data will be deleted, a confirm action, and a cancel action.
3. IF the Operator selects the cancel action on the confirmation dialog, THEN THE app SHALL dismiss the dialog and leave the Local_Product_Store unchanged.
4. WHEN the Operator confirms the deletion, THE Local_Product_Store SHALL delete all stored products and THE app SHALL display a success message indicating the catalog has been cleared.
5. IF the deletion operation fails due to a database error, THEN THE app SHALL display an error message indicating the catalog could not be cleared and SHALL leave any remaining data in the Local_Product_Store intact.

### Requirement 5: Catalog Download Status Visibility

**User Story:** As an Operator, I want to know whether I have products downloaded and how many, so that I can verify I am ready to work offline.

#### Acceptance Criteria

1. THE app SHALL display the count of locally stored products in a visible location on the home screen.
2. WHEN the Local_Product_Store contains zero products, THE app SHALL display a message indicating that no offline catalog is available (e.g., "Sin catálogo offline").
3. WHEN the Local_Product_Store contains one or more products, THE app SHALL display the total product count (e.g., "150 productos disponibles offline").
