# Tasks

> **Aprobación requerida (governance CRITICAL).** No se escribe ningún archivo Java (ni producción ni tests) hasta que la tarea 1.1 esté marcada. Cada grupo sigue TDD estricto: RED (el test falla) → GREEN (mínimo código) → TRIANGULATE (otro caso) → REFACTOR (en verde). Todo test lleva `@DisplayName` con el nombre exacto del escenario de `specs/token-seguro/spec.md`. Los tests son unitarios: JUnit 5 + AssertJ, sin Spring ni base de datos. Comandos Maven desde `backend/` con `JAVA_HOME=C:\Users\santi\.jdks\ms-21.0.12.1`.

## 1. Gate de aprobación y línea base

- [x] 1.1 Obtener la aprobación explícita de Juan Pablo (comentario en el PR o en el issue de C-14) sobre `proposal.md`, `design.md` y `specs/token-seguro/spec.md`, y pegar acá el enlace al comentario. Verificación: el enlace existe y la aprobación es posterior a la última revisión de los artefactos. **Sin esta tarea marcada no se empieza la 1.2.** **Resultado (2026-11-25):** Juan Pablo aprobó la propuesta en forma presencial; lo informó Santino en la sesión. No hay enlace a un comentario: conviene que Juan Pablo deje constancia escrita en el PR de C-14.
- [x] 1.2 Red de seguridad: correr `./mvnw test -Dtest=ArquitecturaTest` y anotar la línea base. Verificación: "1/1 en verde" (la regla `..domain..` de C-01). Si falla, se frena y se reporta como falla preexistente.

## 2. Regla ArchUnit del paquete (design D9)

- [x] 2.1 RED: agregar en `backend/src/test/java/ar/edu/utn/turnos/ArquitecturaTest.java` la regla `el_servicio_de_token_no_depende_de_frameworks` (sin `allowEmptyShould`). Descripción: `..shared.token.. no depende de Spring, JPA ni Hibernate`. Escenario: "El paquete del token no depende de Spring, JPA ni Hibernate". Verificación: `./mvnw test -Dtest=ArquitecturaTest` falla porque el paquete todavía no tiene clases (la regla no puede pasar vacía).
- [x] 2.2 La regla queda en RED hasta que exista la primera clase del paquete (grupo 3). Se confirma en verde en la tarea 3.2. Verificación: anotar en el resumen del apply que el RED de 2.1 se observó.

## 3. Generación de tokens (Requirement: Generación de tokens de 256 bits)

- [x] 3.1 RED: crear `backend/src/test/java/ar/edu/utn/turnos/shared/token/ServicioTokenTest.java` con el test "Un token generado tiene 43 caracteres del alfabeto base64url" (regex `^[A-Za-z0-9_-]{43}$`). Verificación: no compila o falla porque `ServicioToken` y `TokenGenerado` no existen.
- [x] 3.2 GREEN: crear `ServicioToken` (clase `final`, constructor público sin argumentos, `new SecureRandom()`) y `TokenGenerado` (`record` con `claro` y `hash`) en `backend/src/main/java/ar/edu/utn/turnos/shared/token/`, con `generar()` sobre 32 bytes y `Base64.getUrlEncoder().withoutPadding()`. Verificación: el test de 3.1 y la regla ArchUnit de 2.1 pasan.
- [x] 3.3 TRIANGULATE: agregar "Un token generado decodifica a 32 bytes" y "Diez mil tokens generados son todos distintos" (10.000 muestras, `Set` del claro y del hash). Verificación: `./mvnw test -Dtest=ServicioTokenTest` en verde y el test de unicidad tarda menos de 1 s.

## 4. Hash SHA-256 (Requirements: Hash SHA-256 hexadecimal determinista; El token generado incluye su hash)

- [x] 4.1 RED: test "El hash coincide con el vector conocido de SHA-256" (`abc` → `ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad`). Verificación: falla porque `hashear` no existe o tiene un valor falso.
- [x] 4.2 GREEN: implementar `hashear(String)` con `MessageDigest.getInstance("SHA-256")` (uno por llamada) sobre UTF-8 y `HexFormat.of()`. La `NoSuchAlgorithmException` se envuelve en `IllegalStateException`. Verificación: 4.1 en verde.
- [x] 4.3 TRIANGULATE: agregar "El hash de un token es siempre el mismo" (64 hex en minúscula), "Tokens distintos producen hashes distintos" y "El token generado trae el hash de su valor en claro" (`generar()` completa `hash` con `hashear(claro)`). Verificación: `./mvnw test -Dtest=ServicioTokenTest` en verde.
- [x] 4.4 RED → GREEN: "Hashear un valor nulo o en blanco se rechaza" (`@ParameterizedTest` con `null`, `""` y `"   "`) → `IllegalArgumentException` con un mensaje fijo que no contiene el valor recibido. Verificación: falla antes de la validación y pasa después.

## 5. Comparación en tiempo constante (Requirement: Comparación en tiempo constante; design D6 y D10)

- [x] 5.1 RED: test "Un token coincide con su propio hash". Verificación: falla porque `coincide` no existe.
- [x] 5.2 GREEN: implementar `coincide(String claro, String hashGuardado)`: nulo, en blanco o longitud ≠ 64 → `false`; si no, `MessageDigest.isEqual` sobre los bytes US-ASCII de `hashear(claro)` y de `hashGuardado`. Verificación: 5.1 en verde.
- [x] 5.3 TRIANGULATE: agregar "Un token no coincide con el hash de otro token", "Un hash nulo o de otra longitud no coincide" (`null`, `""`, 63 y 65 caracteres) y "Un token nulo o en blanco no coincide". Ninguno lanza. Verificación: `./mvnw test -Dtest=ServicioTokenTest` en verde.
- [x] 5.4 RED → GREEN: test estructural "La comparación usa una primitiva de tiempo constante" con `ClassFileImporter().importClasses(ServicioToken.class)`. Verifica que la clase llama a `MessageDigest.isEqual(byte[], byte[])` y que no llama a `String.equals(Object)`. Para observar el RED, correrlo primero contra una versión temporal que compare con `equals` y después restaurar `isEqual`. Verificación: falla con `equals` y pasa con `isEqual`. No se mide tiempo.

## 6. Enmascarado (Requirement: El token en claro no aparece en su representación de texto; design D7)

- [x] 6.1 RED: crear `backend/src/test/java/ar/edu/utn/turnos/shared/token/TokenGeneradoTest.java` con "El texto del token generado no contiene el token en claro ni su hash" (sobre un `generar()` real: `toString()` no contiene `claro()` ni `hash()` y contiene `***`). Verificación: falla con el `toString` por defecto del `record`.
- [x] 6.2 GREEN: redefinir `toString()` para que devuelva `TokenGenerado[claro=***, hash=***]`. Verificación: 6.1 en verde.
- [x] 6.3 TRIANGULATE: el constructor compacto rechaza `claro` o `hash` nulos con `IllegalArgumentException` (`@DisplayName` "Un token generado rechaza valores nulos", design D7). Verificación: falla antes de agregar la validación y pasa después.

## 7. Refactor y verificación final

- [x] 7.1 REFACTOR: extraer constantes (`BYTES_TOKEN = 32`, `LONGITUD_HASH = 64`, `OCULTO = "***"`) y agregar Javadoc en español que diga que `claro()` nunca se loguea ni se expone como DTO, y que la vigencia y la revocación son de C-32 y C-36. Verificación: `./mvnw test -Dtest=ServicioTokenTest,TokenGeneradoTest,ArquitecturaTest` en verde después de cada paso.
- [x] 7.2 Revisar que el diff no toque nada fuera de `shared/token/`, su test y `ArquitecturaTest`, y que no haya imports de `org.springframework`, `jakarta.persistence` ni `org.hibernate` en el paquete. Verificación: `git diff --stat` y `grep -rn "org.springframework\|jakarta.persistence\|org.hibernate" backend/src/main/java/ar/edu/utn/turnos/shared/token` sin resultados.
- [x] 7.3 Correr la suite completa con `./mvnw verify` (con `JAVA_HOME=C:\Users\santi\.jdks\ms-21.0.12.1`; Docker encendido para los IT existentes). Verificación: BUILD SUCCESS. Si Docker no está disponible, informar los IT como "no ejecutados" y no darlos por pasados.

## Workflow follow-up

- Preguntar antes de cada commit (regla dura 16) y abrir un PR desde la rama `santino` a `master`, que revisa Juan Pablo.
- Correr `openspec-verify` (mapea cada escenario a su `@DisplayName`) y después `/opsx:archive`.
- Marcar C-14 como hecho en CHANGES.md y avisar a Nico (C-32) y a Valentino (C-36) que la API está disponible (`generar`, `hashear`, `coincide`).
