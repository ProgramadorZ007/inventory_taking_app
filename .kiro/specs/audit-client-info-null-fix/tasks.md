# audit-client-info-null-fix — Implementation Tasks

## Tasks

- [x] 1. Agregar interfaz `OnRegistroCallback` en `RegistrarInventarioUseCase`
  - [x] 1.1 Definir la interfaz pública estática `OnRegistroCallback` dentro de `RegistrarInventarioUseCase` con los métodos `onSincronizado()`, `onGuardadoLocal()` y `onError(Exception e)`
  - **File**: `app/src/main/java/com/procesadoraperu/inventario/domain/usecase/inventario/RegistrarInventarioUseCase.java`

- [x] 2. Modificar `RegistrarInventarioUseCase.execute()` para aceptar y propagar el callback
  - [x] 2.1 Cambiar la firma de `execute(Inventario inventario)` a `execute(Inventario inventario, OnRegistroCallback callback)`
  - [x] 2.2 Pasar el `callback` al método `ejecutarGuardadoYLog()` como parámetro adicional
  - **File**: `app/src/main/java/com/procesadoraperu/inventario/domain/usecase/inventario/RegistrarInventarioUseCase.java`

- [x] 3. Invocar el callback desde `ejecutarGuardadoYLog()` al finalizar el flujo
  - [x] 3.1 Agregar `callback` como parámetro de `ejecutarGuardadoYLog(Inventario inventario, OnRegistroCallback callback)`
  - [x] 3.2 Al final del bloque `try` (envío exitoso), llamar `callback.onSincronizado()`
  - [x] 3.3 En el bloque `catch` (guardado local), llamar `callback.onGuardadoLocal()`
  - [x] 3.4 Envolver todo en un try-catch externo para llamar `callback.onError(e)` ante excepciones inesperadas
  - **File**: `app/src/main/java/com/procesadoraperu/inventario/domain/usecase/inventario/RegistrarInventarioUseCase.java`

- [x] 4. Actualizar `TakeInventoryViewModel.registrarInventario()` para usar el callback
  - [x] 4.1 Eliminar la lectura prematura de `inventario.getEstadoSincronizacion()` después de llamar a `execute()`
  - [x] 4.2 Pasar un `OnRegistroCallback` inline a `registrarInventarioUseCase.execute()` que llame a `registroResult.postValue(RegistroResult.SINCRONIZADO)` en `onSincronizado()`
  - [x] 4.3 En `onGuardadoLocal()` del callback, llamar `registroResult.postValue(RegistroResult.GUARDADO_LOCAL)`
  - [x] 4.4 En `onError(e)` del callback, llamar `errorMessage.postValue(...)` y `registroResult.postValue(RegistroResult.ERROR)`
  - [x] 4.5 Mover `isRegistrando.postValue(false)` y `isRegistrando.postValue(false)` del bloque `finally` al interior de cada rama del callback (onSincronizado, onGuardadoLocal, onError)
  - [x] 4.6 Eliminar el bloque `try/catch/finally` externo del executor que ya no es necesario para leer el resultado
  - **File**: `app/src/main/java/com/procesadoraperu/inventario/presentation/inventory/take/TakeInventoryViewModel.java`

- [x] 5. Escribir tests exploratorios (bug condition checking — correr en código SIN fix)
  - [x] 5.1 Crear clase de test `RegistrarInventarioUseCaseExploratoryTest` con un mock de `IAuditClientInfoProvider` que retarda el callback 200ms
  - [x] 5.2 Verificar que `inventario.getAuditClientInfo()` es `null` inmediatamente después de que `execute()` retorna (demuestra el bug)
  - [x] 5.3 Verificar que el request capturado por el mock de `InventarioApi` tiene `auditClientInfo = null` (demuestra el bug en el envío HTTP)
  - **File**: `app/src/test/java/com/procesadoraperu/inventario/domain/usecase/inventario/RegistrarInventarioUseCaseExploratoryTest.java`

- [x] 6. Escribir tests de fix checking (verificar que el fix funciona)
  - [x] 6.1 Crear clase de test `RegistrarInventarioUseCaseFixTest`
  - [x] 6.2 Test: con callback retardado 200ms, verificar que `onSincronizado()` se invoca y `auditClientInfo` está poblado en el request
  - [x] 6.3 Test: con mock de red que lanza excepción, verificar que `onGuardadoLocal()` se invoca y el inventario se guarda con `auditClientInfo` poblado
  - [x] 6.4 Test: con `getAuditInfo()` que retorna inmediatamente sin GPS (lat/lon vacíos), verificar que `auditClientInfo` tiene los campos de hardware correctos
  - **File**: `app/src/test/java/com/procesadoraperu/inventario/domain/usecase/inventario/RegistrarInventarioUseCaseFixTest.java`

- [x] 7. Escribir tests de preservation checking (verificar que no hay regresiones)
  - [x] 7.1 Crear clase de test `RegistrarInventarioUseCasePreservationTest`
  - [x] 7.2 Test: verificar que todos los campos del request (idEmpresa, idSucursal, idAlmacen, idProducto, dscProducto, idMedida, stock, cantidad) se envían correctamente después del fix
  - [x] 7.3 Test: verificar que el log de integración se guarda correctamente tanto en éxito como en error
  - [x] 7.4 Test: verificar que el callback `onRegistroCallback` se invoca exactamente una vez por llamada a `execute()`
  - [x] 7.5 Test: verificar que cuando no hay permisos GPS, el registro continúa con coordenadas vacías (no se bloquea)
  - **File**: `app/src/test/java/com/procesadoraperu/inventario/domain/usecase/inventario/RegistrarInventarioUseCasePreservationTest.java`
