# Bugfix Requirements Document

## Introduction

Al registrar un inventario desde la app Android, el campo `auditClientInfo` llega como `null` al servidor en lugar de contener los datos del dispositivo (dispositivo, ip, hostname, userAgent, latitud, longitud). Esto impide el correcto registro de auditoría de las operaciones de inventario.

La causa raíz es una condición de carrera asíncrona: `RegistrarInventarioUseCase.execute()` delega la obtención de datos del dispositivo a `AuditClientInfoProvider.getAuditInfo()`, cuyo callback es asíncrono (puede retornar en el MainThread). Sin embargo, `TakeInventoryViewModel.registrarInventario()` llama a `registrarInventarioUseCase.execute(inventario)` dentro de un `executor.execute()` y luego lee `inventario.getEstadoSincronizacion()` inmediatamente después, antes de que el callback asíncrono haya completado y seteado el `auditClientInfo` en el objeto `inventario`. Como resultado, el envío al servidor ocurre con `auditClientInfo = null`.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN el usuario registra un inventario y `AuditClientInfoProvider.getAuditInfo()` aún no ha completado su callback asíncrono THEN el sistema envía la solicitud HTTP al servidor con el campo `auditClientInfo` en `null`

1.2 WHEN `RegistrarInventarioUseCase.execute()` es invocado desde un hilo de fondo del ViewModel THEN el sistema inicia la obtención de datos de auditoría de forma asíncrona pero continúa el flujo sin esperar el resultado, provocando que `inventario.getAuditClientInfo()` sea `null` al momento del envío

1.3 WHEN el callback de `getAuditInfo()` retorna en el MainThread y llama a `executor.execute()` para el guardado THEN el sistema ejecuta el guardado/envío en un hilo separado, pero el hilo original del ViewModel ya verificó `inventario.getEstadoSincronizacion()` antes de que este trabajo completara

### Expected Behavior (Correct)

2.1 WHEN el usuario registra un inventario THEN el sistema SHALL esperar a que `AuditClientInfoProvider.getAuditInfo()` complete su callback antes de enviar la solicitud HTTP al servidor

2.2 WHEN `RegistrarInventarioUseCase.execute()` es invocado THEN el sistema SHALL garantizar que `inventario.getAuditClientInfo()` contiene los datos del dispositivo (dispositivo, ip, hostname, userAgent) antes de llamar a `inventarioRepository.enviarInventarioRemote(inventario)`

2.3 WHEN el envío al servidor es exitoso THEN el sistema SHALL incluir en el cuerpo JSON el objeto `auditClientInfo` con los campos dispositivo, ip, hostname y userAgent correctamente poblados

2.4 WHEN no se puede obtener la ubicación GPS THEN el sistema SHALL enviar `auditClientInfo` con los campos de hardware (dispositivo, ip, hostname, userAgent) correctamente poblados y latitud/longitud como `null` o vacíos, sin bloquear el registro

### Unchanged Behavior (Regression Prevention)

3.1 WHEN el dispositivo no tiene permisos de ubicación THEN el sistema SHALL CONTINUE TO registrar el inventario enviando `auditClientInfo` con los datos de hardware disponibles y coordenadas vacías

3.2 WHEN el envío al servidor falla por falta de conectividad THEN el sistema SHALL CONTINUE TO guardar el inventario localmente con estado `PENDIENTE`, incluyendo el `auditClientInfo` en la entidad local

3.3 WHEN el inventario se guarda localmente y luego se sincroniza THEN el sistema SHALL CONTINUE TO sincronizar correctamente los registros pendientes

3.4 WHEN se registra un inventario con cantidad positiva válida THEN el sistema SHALL CONTINUE TO enviar todos los demás campos del request (idEmpresa, idSucursal, idAlmacen, idProducto, dscProducto, idMedida, stock, cantidad) correctamente

3.5 WHEN el ViewModel recibe el resultado del registro THEN el sistema SHALL CONTINUE TO notificar el estado correcto (SINCRONIZADO o GUARDADO_LOCAL) a la UI mediante LiveData

---

## Bug Condition (Pseudocódigo)

**Función de condición del bug** — identifica las entradas que disparan el bug:

```pascal
FUNCTION isBugCondition(inventario, auditCallbackCompletado)
  INPUT: inventario de tipo Inventario, auditCallbackCompletado de tipo boolean
  OUTPUT: boolean

  // El bug ocurre cuando el envío al servidor se realiza
  // antes de que el callback asíncrono de auditoría haya completado
  RETURN inventario.getAuditClientInfo() = null
     AND auditCallbackCompletado = false
END FUNCTION
```

**Propiedad: Fix Checking** — comportamiento correcto para entradas con bug:

```pascal
// Property: Fix Checking - auditClientInfo debe estar poblado al enviar
FOR ALL inventario WHERE isBugCondition(inventario, auditCallbackCompletado) DO
  result ← registrarInventario'(inventario)
  ASSERT result.requestEnviado.auditClientInfo ≠ null
  ASSERT result.requestEnviado.auditClientInfo.dispositivo ≠ null
  ASSERT result.requestEnviado.auditClientInfo.ip ≠ null
END FOR
```

**Propiedad: Preservation Checking** — entradas no afectadas por el bug:

```pascal
// Property: Preservation Checking
FOR ALL inventario WHERE NOT isBugCondition(inventario, auditCallbackCompletado) DO
  ASSERT registrarInventario(inventario) = registrarInventario'(inventario)
END FOR
```
