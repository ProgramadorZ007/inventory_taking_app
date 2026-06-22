# audit-client-info-null-fix Bugfix Design

## Overview

Al registrar un inventario, el campo `auditClientInfo` llega como `null` al servidor porque `TakeInventoryViewModel.registrarInventario()` lee `inventario.getEstadoSincronizacion()` inmediatamente después de llamar a `registrarInventarioUseCase.execute(inventario)`, sin esperar a que el callback asíncrono de `AuditClientInfoProvider.getAuditInfo()` complete.

El flujo actual tiene dos problemas encadenados:

1. **`RegistrarInventarioUseCase.execute()`** ya fue parcialmente corregido: el envío al servidor ocurre dentro del callback de `getAuditInfo()`. Sin embargo, el método `execute()` retorna inmediatamente (antes de que el callback dispare), por lo que el ViewModel no puede saber cuándo terminó.

2. **`TakeInventoryViewModel.registrarInventario()`** lee `inventario.getEstadoSincronizacion()` justo después de llamar a `execute()`, cuando el callback aún no ha completado. Esto produce que el ViewModel siempre vea `estadoSincronizacion = null` y reporte `GUARDADO_LOCAL` incorrectamente, además de que el envío HTTP ocurre con `auditClientInfo = null` si la versión anterior del use case no tenía la corrección.

La estrategia de fix es:
- Refactorizar `RegistrarInventarioUseCase` para que acepte un **callback de resultado** (`OnRegistroCallback`) que se invoque cuando el flujo completo (auditoría + envío/guardado) haya terminado.
- Actualizar `TakeInventoryViewModel` para leer el resultado desde ese callback en lugar de leer el estado del objeto `inventario` de forma prematura.

---

## Glossary

- **Bug_Condition (C)**: La condición que dispara el bug — cuando `TakeInventoryViewModel` lee `inventario.getEstadoSincronizacion()` antes de que el callback de `getAuditInfo()` haya completado, resultando en `auditClientInfo = null` en el request HTTP.
- **Property (P)**: El comportamiento correcto — el request HTTP al servidor DEBE incluir `auditClientInfo` con los datos del dispositivo (dispositivo, ip, hostname, userAgent) correctamente poblados.
- **Preservation**: El comportamiento existente que NO debe cambiar — guardado local en modo offline, notificación de estado a la UI, registro de logs, y todos los demás campos del request.
- **`RegistrarInventarioUseCase`**: Use case en `domain/usecase/inventario/RegistrarInventarioUseCase.java` que orquesta la obtención de datos de auditoría, el envío al servidor y el guardado local.
- **`TakeInventoryViewModel`**: ViewModel en `presentation/inventory/take/TakeInventoryViewModel.java` que construye el objeto `Inventario` y delega el registro al use case.
- **`AuditClientInfoProvider`**: Proveedor en `core/location/AuditClientInfoProvider.java` que obtiene datos del dispositivo (GPS, IP, hardware) de forma asíncrona mediante un callback.
- **`IAuditClientInfoProvider.OnAuditInfoCallback`**: Interfaz de callback que notifica cuando los datos de auditoría están disponibles.
- **`estadoSincronizacion`**: Campo del objeto `Inventario` que indica el resultado del registro (`SINCRONIZADO` o `PENDIENTE`). Solo es válido leerlo DESPUÉS de que el callback de auditoría y el flujo de red/guardado hayan completado.
- **Race condition**: Condición de carrera donde dos operaciones concurrentes acceden a un recurso compartido sin sincronización adecuada.

---

## Bug Details

### Bug Condition

El bug se manifiesta cuando `TakeInventoryViewModel.registrarInventario()` llama a `registrarInventarioUseCase.execute(inventario)` y luego lee `inventario.getEstadoSincronizacion()` en el mismo bloque `executor.execute()`, sin esperar a que el callback asíncrono de `AuditClientInfoProvider.getAuditInfo()` complete. El método `execute()` del use case retorna inmediatamente después de llamar a `getAuditInfo()`, antes de que el callback dispare.

**Formal Specification:**

```
FUNCTION isBugCondition(inventario, auditCallbackCompletado)
  INPUT: inventario de tipo Inventario
         auditCallbackCompletado de tipo boolean
  OUTPUT: boolean

  // El bug ocurre cuando el ViewModel lee el estado del inventario
  // antes de que el callback asíncrono de auditoría haya completado
  RETURN inventario.getAuditClientInfo() = null
     AND auditCallbackCompletado = false
END FUNCTION
```

### Examples

- **Caso normal (bug activo)**: Usuario registra inventario con conexión a internet → `execute()` retorna → ViewModel lee `estadoSincronizacion = null` → reporta `GUARDADO_LOCAL` incorrectamente → el callback dispara después y envía con `auditClientInfo = null`.
- **Caso offline (bug activo)**: Usuario registra sin conexión → mismo problema de timing → el inventario se guarda localmente pero `auditClientInfo` puede ser `null` en la entidad guardada.
- **Caso sin permisos GPS (bug activo)**: `getAuditInfo()` retorna inmediatamente con datos de hardware → pero si el ViewModel ya leyó el estado antes, el resultado sigue siendo incorrecto.
- **Caso con GPS lento (bug más severo)**: `getAuditInfo()` tarda hasta 16 segundos (3 intentos en cascada) → la ventana de la race condition es máxima → `auditClientInfo` definitivamente es `null` al momento del envío.

---

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Los clics del mouse/touch en botones de la UI deben continuar funcionando exactamente igual.
- El guardado local con estado `PENDIENTE` cuando no hay conectividad debe continuar funcionando.
- La sincronización posterior de registros pendientes (`SincronizarPendientesUseCase`) no debe verse afectada.
- El registro de logs de integración (`LogIntegracion`) debe continuar funcionando igual.
- La notificación de estado a la UI mediante `LiveData<RegistroResult>` debe continuar funcionando.
- Todos los demás campos del request (`idEmpresa`, `idSucursal`, `idAlmacen`, `idProducto`, `dscProducto`, `idMedida`, `stock`, `cantidad`) deben enviarse correctamente.
- Cuando no se puede obtener ubicación GPS, `auditClientInfo` debe enviarse con los campos de hardware y coordenadas vacías (sin bloquear el registro).

**Scope:**
Todos los flujos que NO involucran el registro de inventario (búsqueda de productos, historial, sincronización de pendientes, autenticación) deben quedar completamente sin cambios.

---

## Hypothesized Root Cause

Basado en el análisis del código fuente:

1. **`TakeInventoryViewModel` lee el resultado prematuramente**: En `registrarInventario()`, después de llamar a `registrarInventarioUseCase.execute(inventario)`, el ViewModel lee `inventario.getEstadoSincronizacion()` en la misma lambda del `executor.execute()`. Como `execute()` retorna inmediatamente (el callback de auditoría es asíncrono), `estadoSincronizacion` aún es `null` en ese punto.

2. **`RegistrarInventarioUseCase.execute()` no tiene mecanismo de notificación de resultado**: El método es `void` y no provee ningún callback ni `Future` para que el llamador sepa cuándo terminó el flujo completo. El ViewModel no tiene forma de esperar el resultado correctamente.

3. **Mutación de estado compartido entre hilos**: El objeto `Inventario` es mutado por el callback de auditoría (en el MainThread) y leído por el ViewModel (en el hilo del executor). Aunque en la práctica el ViewModel lee antes de que el callback dispare, este patrón de mutación compartida es inherentemente frágil.

4. **`AuditClientInfoProvider` puede tardar hasta 16 segundos**: Con los 3 intentos en cascada (getLastLocation → getCurrentLocation con timeout 6s → requestLocationUpdates con timeout 10s), la ventana de la race condition puede ser muy amplia, garantizando que el ViewModel siempre lea el estado antes de que el callback complete.

---

## Correctness Properties

Property 1: Bug Condition - auditClientInfo Poblado al Enviar

_For any_ invocación de `registrarInventario` donde el usuario registra un inventario con conectividad disponible, el use case corregido SHALL garantizar que `inventario.getAuditClientInfo()` es no-null y contiene los campos `dispositivo`, `ip`, `hostname` y `userAgent` correctamente poblados en el momento en que se llama a `inventarioRepository.enviarInventarioRemote(inventario)`.

**Validates: Requirements 2.1, 2.2, 2.3**

Property 2: Preservation - Comportamiento No-Buggy Sin Cambios

_For any_ flujo que NO involucra el timing de la obtención de auditoría (guardado local offline, sincronización de pendientes, búsqueda de productos, historial), el código corregido SHALL producir exactamente el mismo resultado que el código original, preservando todos los comportamientos existentes de guardado, notificación de UI y registro de logs.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**

---

## Fix Implementation

### Changes Required

Asumiendo que la causa raíz es correcta:

**Archivo 1**: `app/src/main/java/com/procesadoraperu/inventario/domain/usecase/inventario/RegistrarInventarioUseCase.java`

**Cambios específicos**:

1. **Agregar interfaz de callback de resultado**: Definir `OnRegistroCallback` con métodos `onSincronizado()`, `onGuardadoLocal()` y `onError(Exception)` para notificar al llamador cuando el flujo completo termine.

2. **Modificar la firma de `execute()`**: Cambiar de `void execute(Inventario inventario)` a `void execute(Inventario inventario, OnRegistroCallback callback)` para recibir el callback de resultado.

3. **Invocar el callback desde `ejecutarGuardadoYLog()`**: Al final del bloque `try` (éxito) llamar `callback.onSincronizado()`, en el bloque `catch` (guardado local) llamar `callback.onGuardadoLocal()`, y en caso de error inesperado llamar `callback.onError(e)`.

**Archivo 2**: `app/src/main/java/com/procesadoraperu/inventario/presentation/inventory/take/TakeInventoryViewModel.java`

**Cambios específicos**:

4. **Eliminar la lectura prematura de `estadoSincronizacion`**: Remover el bloque que lee `inventario.getEstadoSincronizacion()` después de llamar a `execute()`.

5. **Pasar un `OnRegistroCallback` a `execute()`**: Implementar el callback inline que llame a `registroResult.postValue(RegistroResult.SINCRONIZADO)`, `registroResult.postValue(RegistroResult.GUARDADO_LOCAL)` o `registroResult.postValue(RegistroResult.ERROR)` según corresponda.

6. **Mover `isRegistrando.postValue(false)` al callback**: El indicador de carga debe desactivarse cuando el flujo completo termine, no cuando `execute()` retorne.

### Pseudocódigo del Fix

```
// RegistrarInventarioUseCase — DESPUÉS del fix
INTERFACE OnRegistroCallback
  onSincronizado()
  onGuardadoLocal()
  onError(Exception e)
END INTERFACE

FUNCTION execute(inventario, callback)
  auditProvider.getAuditInfo(auditInfo -> {
    inventario.setAuditClientInfo(auditInfo)
    executor.execute(() -> ejecutarGuardadoYLog(inventario, callback))
  })
END FUNCTION

FUNCTION ejecutarGuardadoYLog(inventario, callback)
  TRY
    inventarioRepository.enviarInventarioRemote(inventario)  // auditClientInfo ya está poblado
    logRepository.saveLogLocal(logExito)
    callback.onSincronizado()
  CATCH Exception e
    inventarioRepository.saveInventarioLocal(inventario)
    logRepository.saveLogLocal(logError)
    callback.onGuardadoLocal()
  END TRY
END FUNCTION

// TakeInventoryViewModel — DESPUÉS del fix
FUNCTION registrarInventario(producto, cantidadContada)
  isRegistrando.postValue(true)
  executor.execute(() -> {
    // ... construir inventario ...
    registrarInventarioUseCase.execute(inventario, new OnRegistroCallback() {
      onSincronizado() -> {
        registroResult.postValue(SINCRONIZADO)
        isRegistrando.postValue(false)
      }
      onGuardadoLocal() -> {
        registroResult.postValue(GUARDADO_LOCAL)
        isRegistrando.postValue(false)
      }
      onError(e) -> {
        errorMessage.postValue("Error: " + e.getMessage())
        registroResult.postValue(ERROR)
        isRegistrando.postValue(false)
      }
    })
    // NO leer inventario.getEstadoSincronizacion() aquí
  })
END FUNCTION
```

---

## Testing Strategy

### Validation Approach

La estrategia de testing sigue un enfoque de dos fases: primero, demostrar el bug en el código sin corregir mediante tests exploratorios; luego, verificar que el fix funciona correctamente y que los comportamientos existentes se preservan.

### Exploratory Bug Condition Checking

**Goal**: Demostrar el bug ANTES de implementar el fix. Confirmar o refutar el análisis de causa raíz. Si se refuta, se deberá re-hipotizar.

**Test Plan**: Escribir tests unitarios que simulen el flujo de `RegistrarInventarioUseCase.execute()` con un `AuditClientInfoProvider` que retarda el callback (simulando GPS lento), y verificar que el request enviado al servidor tiene `auditClientInfo = null` en el código sin corregir.

**Test Cases**:
1. **Test de race condition con callback retardado**: Crear un mock de `IAuditClientInfoProvider` que retarda el callback 100ms, llamar a `execute()`, verificar inmediatamente que `inventario.getAuditClientInfo()` es null (demostrará el bug en el código original).
2. **Test de lectura prematura en ViewModel**: Verificar que `TakeInventoryViewModel` reporta `GUARDADO_LOCAL` incluso cuando hay conectividad, porque lee el estado antes de que el callback complete.
3. **Test de request con auditClientInfo null**: Capturar el request enviado a `InventarioApi` y verificar que `auditClientInfo` es null (bug confirmado).
4. **Test de callback inmediato (sin GPS)**: Con un mock que retorna inmediatamente (sin permisos GPS), verificar si el bug se manifiesta o no (puede no manifestarse en este caso).

**Expected Counterexamples**:
- `inventario.getAuditClientInfo()` es `null` cuando el callback tarda más de 0ms.
- El request HTTP se envía con `auditClientInfo = null`.
- Posibles causas: `execute()` retorna antes de que el callback dispare, el ViewModel lee el estado prematuramente.

### Fix Checking

**Goal**: Verificar que para todas las entradas donde la condición del bug se cumple, el use case corregido produce el comportamiento esperado.

**Pseudocode:**
```
FOR ALL inventario WHERE isBugCondition(inventario, auditCallbackCompletado = false) DO
  result := registrarInventarioUseCase_fixed.execute(inventario, callback)
  WAIT FOR callback.onSincronizado() OR callback.onGuardadoLocal()
  ASSERT inventario.getAuditClientInfo() ≠ null
  ASSERT inventario.getAuditClientInfo().getDispositivo() ≠ null
  ASSERT inventario.getAuditClientInfo().getIp() ≠ null
  ASSERT requestCapturado.auditClientInfo ≠ null
END FOR
```

### Preservation Checking

**Goal**: Verificar que para todas las entradas donde la condición del bug NO se cumple, el código corregido produce el mismo resultado que el código original.

**Pseudocode:**
```
FOR ALL inventario WHERE NOT isBugCondition(inventario, auditCallbackCompletado) DO
  ASSERT registrarInventario_original(inventario) = registrarInventario_fixed(inventario, callback)
END FOR
```

**Testing Approach**: Se recomienda property-based testing para preservation checking porque:
- Genera muchos casos de prueba automáticamente a través del dominio de entrada.
- Captura edge cases que los tests unitarios manuales podrían omitir.
- Provee garantías fuertes de que el comportamiento no cambia para entradas no-buggy.

**Test Plan**: Observar el comportamiento en el código SIN corregir para flujos offline y de sincronización, luego escribir tests que capturen ese comportamiento y verificar que se preserva después del fix.

**Test Cases**:
1. **Preservation del guardado local offline**: Verificar que cuando no hay conectividad, el inventario se guarda localmente con `estadoSincronizacion = PENDIENTE` y `auditClientInfo` poblado.
2. **Preservation de la notificación de UI**: Verificar que `registroResult` LiveData recibe `SINCRONIZADO` o `GUARDADO_LOCAL` correctamente después del fix.
3. **Preservation de los campos del request**: Verificar que todos los campos del request (idEmpresa, idSucursal, etc.) siguen siendo correctos después del fix.
4. **Preservation del registro de logs**: Verificar que `LogIntegracion` se guarda correctamente tanto en éxito como en error.

### Unit Tests

- Test de `RegistrarInventarioUseCase` con mock de `IAuditClientInfoProvider` que retarda el callback.
- Test de `RegistrarInventarioUseCase` con mock que retorna inmediatamente (sin GPS).
- Test de `RegistrarInventarioUseCase` con mock de `IInventarioRepository` que lanza excepción (modo offline).
- Test de `TakeInventoryViewModel.registrarInventario()` verificando que `registroResult` se actualiza desde el callback.
- Test de edge case: `getAuditInfo()` retorna con latitud/longitud vacíos (sin permisos GPS).

### Property-Based Tests

- Generar estados de inventario aleatorios y verificar que `auditClientInfo` siempre está poblado en el request enviado al servidor.
- Generar configuraciones de red aleatorias (online/offline) y verificar que el callback de resultado siempre se invoca exactamente una vez.
- Verificar que para cualquier `AuditClientInfo` válido, todos sus campos se mapean correctamente al `RegistrarInventarioRequest`.

### Integration Tests

- Test del flujo completo: `TakeInventoryViewModel` → `RegistrarInventarioUseCase` → `AuditClientInfoProvider` (mock) → `InventarioApi` (mock) → verificar request completo.
- Test de flujo offline: verificar que el inventario se guarda en Room con `auditClientInfo` poblado.
- Test de flujo con GPS lento: simular callback retardado 5 segundos y verificar que el request espera correctamente.
