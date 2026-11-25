# Verify — c-14-servicio-token-seguro (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR (0 bloqueantes; 1 observación para el PR)

## Validación estructural
`openspec validate c-14-servicio-token-seguro --strict` → "Change 'c-14-servicio-token-seguro' is valid".

## Tareas
21/21 completas (`[x]`). Sin pendientes.

Observación (no es falla): la tarea 1.1 (aprobación de Juan Pablo, governance CRITICAL) está marcada con base en una aprobación presencial informada por Santino el 2026-11-25. No hay enlace a un comentario escrito. El PR debería llevar la aprobación por escrito de Juan Pablo.

## Matriz escenario → test
Todos los tests de `ServicioTokenTest` / `TokenGeneradoTest` llevan `@DisplayName` con el nombre exacto del escenario.

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| token-seguro | Un token generado tiene 43 caracteres del alfabeto base64url | Cubierto | ServicioTokenTest.java:26 | |
| token-seguro | Un token generado decodifica a 32 bytes | Cubierto | ServicioTokenTest.java:34 | |
| token-seguro | Diez mil tokens generados son todos distintos | Cubierto | ServicioTokenTest.java:42 | claro y hash |
| token-seguro | El token generado trae el hash de su valor en claro | Cubierto | ServicioTokenTest.java:81 | |
| token-seguro | Un token generado rechaza valores nulos | Cubierto | TokenGeneradoTest.java:22 | |
| token-seguro | El hash de un token es siempre el mismo | Cubierto | ServicioTokenTest.java:64 | 64 hex minúscula |
| token-seguro | El hash coincide con el vector conocido de SHA-256 | Cubierto | ServicioTokenTest.java:57 | `abc` |
| token-seguro | Tokens distintos producen hashes distintos | Cubierto | ServicioTokenTest.java:75 | |
| token-seguro | Hashear un valor nulo o en blanco se rechaza | Cubierto | ServicioTokenTest.java:91 | parametrizado null/""/blanco |
| token-seguro | Un token coincide con su propio hash | Cubierto | ServicioTokenTest.java:99 | |
| token-seguro | Un token no coincide con el hash de otro token | Cubierto | ServicioTokenTest.java:107 | |
| token-seguro | Un hash nulo o de otra longitud no coincide | Cubierto | ServicioTokenTest.java:118 | |
| token-seguro | Un token nulo o en blanco no coincide | Cubierto | ServicioTokenTest.java:128 | |
| token-seguro | La comparación usa una primitiva de tiempo constante | Cubierto | ServicioTokenTest.java:136 | ArchUnit: llama `MessageDigest.isEqual(byte[],byte[])`, no `String.equals` |
| token-seguro | El texto del token generado no contiene el token en claro ni su hash | Cubierto | TokenGeneradoTest.java:12 | |
| token-seguro | El paquete del token no depende de Spring, JPA ni Hibernate | Cubierto | ArquitecturaTest.java:27 (`el_servicio_de_token_no_depende_de_frameworks`) | usa `.as("..shared.token.. no depende de Spring, JPA ni Hibernate")` en vez de `@DisplayName` (convención aceptada de C-01); sin `allowEmptyShould` |

Cobertura: 16/16 Cubiertos, 0 Parciales, 0 Sin test.

## Ejecución de tests
Comando: `./mvnw verify` desde `backend/` (JDK 21 ms-21.0.12.1, Docker disponible).
- Surefire: `Tests run: 132, Failures: 0, Errors: 0, Skipped: 0` (ServicioTokenTest 20, TokenGeneradoTest 2, ArquitecturaTest 2).
- Failsafe: `Tests run: 342, Failures: 0, Errors: 0, Skipped: 0`.
- `BUILD SUCCESS`. Los "ERROR duplicate key" del log son de ITs de otros equipos que prueban constraints a propósito; no son fallas.

## Reglas de negocio y reglas duras
- RN-25 / DD-06: 32 bytes de `SecureRandom`, base64url sin relleno (43), solo hash SHA-256, comparación en tiempo constante. Consistente.
- RN-38: mismo servicio y política para tokens de oferta; la API (`generar`, `hashear`, `coincide`) alcanza para C-36. RN-07 (alcance/vigencia): fuera de alcance de C-14, delegado a C-32/C-36 y documentado en el Javadoc.
- Regla dura 3 (token no en claro): el servicio entrega `{claro, hash}` para que el consumidor persista solo el hash; `toString` enmascarado; los mensajes de error no incluyen el valor. OK. No hay persistencia en este change.
- Regla dura 11 (@DisplayName exacto): OK (ver matriz; el escenario ArchUnit usa `.as`).
- Regla dura 17 (español): clases, métodos, Javadoc y mensajes en español. OK.
- Sin datos reales ni secretos: los tests usan solo `abc` y tokens generados. No se leyeron archivos `.env`.
- Imports de `shared.token`: solo JDK (`java.security`, `java.util`, `java.nio`); `grep` de `springframework|jakarta.persistence|hibernate` sin resultados.
- Diff acotado: `git status --short` muestra solo `ArquitecturaTest.java` (modificado, +11 líneas, la regla de C-01 intacta), `shared/token/` (main y test) y `openspec/changes/c-14-servicio-token-seguro/`. `frontend/.vite/` aparece sin seguimiento: es ajeno al change (no se tocó).

## Diferencias con design.md
Ninguna bloqueante ni mayor.
- API: `ServicioToken` final, constructor sin argumentos, `generar()`, `hashear(String)`, `coincide(String,String)`; `TokenGenerado` record con constructor compacto que rechaza nulos. Coincide con el diseño.
- D3: `new SecureRandom()` (no `getInstanceStrong`), una instancia compartida. D4: `Base64.getUrlEncoder().withoutPadding()` sobre 32 bytes. D5: SHA-256 UTF-8, `HexFormat.of()` (minúscula), un `MessageDigest` por llamada, `NoSuchAlgorithmException` envuelta en `IllegalStateException`. D6: nulo/blanco/longitud ≠ 64 → `false`; si no, `MessageDigest.isEqual` sobre bytes US-ASCII. D7: `toString` = `TokenGenerado[claro=***, hash=***]`. D8: mensaje fijo "El token no puede estar vacío" sin el valor. D9: regla ArchUnit sin `allowEmptyShould`.
- Menor: la regla ArchUnit usa `.as(...)` en lugar de `@DisplayName`; está aceptado.

## Qué NO se pudo verificar
- El RED observado de las tareas 2.1 y 5.4 (regla con paquete vacío; test estructural contra versión con `equals`) se reporta en el apply, no se reprodujo acá (verify no modifica código).
- Aprobación escrita de Juan Pablo (no existe todavía).
- Uso real por C-32/C-36 (logs sin token, persistencia solo del hash): corresponde a esos changes.

## Próximo paso
Pedir a Juan Pablo que deje su aprobación escrita en el PR de C-14, preguntar antes de commitear (regla dura 16), abrir PR de `santino` a `master` y luego `/opsx:archive c-14-servicio-token-seguro`. Después, marcar C-14 en CHANGES.md y avisar a Nico (C-32) y Valentino (C-36).
