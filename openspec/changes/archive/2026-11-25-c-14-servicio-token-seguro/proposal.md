# Proposal

> **Aprobación requerida (governance CRITICAL).** Este change solo contiene análisis y propuesta. No se escribe código de producción ni tests hasta que Juan Pablo apruebe explícitamente estos artefactos con un comentario en el PR o en el issue de C-14 (CHANGES.md, ficha `[C-14]`).

## Why

El paciente no tiene cuenta: todo lo que puede hacer sobre su turno (ver, confirmar, cancelar) y sobre una oferta de hueco de la lista de espera (ver y tomar) lo hace con un enlace que lleva un token (RN-07, RN-25, RN-38, DD-06). Si ese token se pudiera adivinar, se filtrara por un log o se pudiera reconstruir desde la base, un tercero podría cancelar turnos ajenos o quedarse con huecos. Hace falta **una sola pieza** que genere, hashee y compare esos tokens de forma correcta, para que C-32 (token del turno) y C-36 (token de la oferta) la reutilicen sin reimplementar criptografía. Se hace ahora porque solo depende de C-01 (mergeado) y bloquea a C-32 y C-36.

## What Changes

- Nuevo servicio de dominio puro `ar.edu.utn.turnos.shared.token.ServicioToken`, sin Spring ni JPA:
  - genera tokens con `SecureRandom` de 256 bits (32 bytes) codificados en base64url sin relleno (43 caracteres);
  - calcula el hash SHA-256 de un token en hexadecimal (64 caracteres);
  - compara un token en claro con un hash guardado en tiempo constante.
- Nuevo tipo `ar.edu.utn.turnos.shared.token.TokenGenerado {claro, hash}` con `toString` enmascarado, para que el token nunca llegue a un log aunque alguien registre el objeto entero.
- Tests unitarios (JUnit 5 + AssertJ, sin contexto de Spring): longitud y alfabeto, unicidad sobre miles de muestras, hash determinista, comparación en tiempo constante y enmascarado.
- Nueva regla ArchUnit en `ArquitecturaTest` para que `..shared.token..` no dependa de Spring, JPA ni Hibernate (la regla de C-01 solo cubre `..domain..`).

## Capabilities

### New Capabilities
- `token-seguro`: generación, hash y comparación de los tokens secretos que le dan al paciente acceso sin login (token de turno y token de oferta de hueco), y la garantía de que el valor en claro no se expone al convertirse en texto.

### Modified Capabilities
- Ninguna. No cambia ningún requisito de las specs existentes.

## Impact

- **Código nuevo:** paquete `backend/src/main/java/ar/edu/utn/turnos/shared/token/` (dos tipos) y su test en `backend/src/test/java/ar/edu/utn/turnos/shared/token/`.
- **Código modificado:** `backend/src/test/java/ar/edu/utn/turnos/ArquitecturaTest.java` (una regla nueva; la existente no se toca).
- **Dependencias:** ninguna nueva. Se usan `java.security.SecureRandom`, `java.security.MessageDigest`, `java.util.Base64` y `java.util.HexFormat` del JDK 21; ArchUnit ya está en el `pom.xml` desde C-01.
- **API HTTP, base de datos, configuración y frontend:** sin cambios.
- **Consumidores:** C-32 (`ServicioTokenTurno`, en `reservapublica.token`) y C-36 (`OfertaHueco`, en `listaespera.oferta`).

## Fuera de alcance

Según las fichas de CHANGES.md, todo esto pertenece a otros changes y **no** se hace acá:

- Persistencia del hash, entidades `TokenTurno` y `OfertaHueco`, unicidad de `token_hash` (C-32, C-36).
- Vigencia, expiración al inicio del turno o del hueco, revocación, un solo uso y `ultimo_uso_en` (C-32, C-36).
- Respuesta 404 uniforme, endpoints públicos y rate limit (C-32, C-38, C-39; el rate limit ya existe desde C-17).
- Armado del enlace del email y envío (C-15, C-31, C-36).
- Test que captura logs reales de la aplicación y verifica que el token no aparece (C-32, C-36). Acá solo se garantiza el enmascarado de `TokenGenerado`.
- Registro de `ServicioToken` como bean de Spring: el paquete no puede importar Spring; el consumidor lo instancia o lo declara en su propia configuración.
- Pepper/HMAC, sal, rotación de tokens o un formato con versión: ni la KB ni la ficha los piden.

## Aprobación requerida

- Governance **CRITICAL** (CHANGES.md): sin código hasta la aprobación explícita de Juan Pablo.
- La primera tarea de `tasks.md` es ese gate. Mientras no esté marcada con el enlace al comentario de aprobación, `/opsx:apply` no debe empezar.
