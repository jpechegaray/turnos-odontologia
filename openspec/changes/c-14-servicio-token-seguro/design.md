# Design

> **Aprobación requerida (governance CRITICAL).** Este diseño es para que Juan Pablo lo revise. No se implementa hasta su aprobación explícita (ver `tasks.md`, tarea 1.1).

## Context

- La motivación está en `proposal.md` (§Why). Los requisitos y escenarios están en `specs/token-seguro/spec.md`.
- La KB fija la política sin margen: `SecureRandom` de 256 bits, base64url (43 caracteres), solo se persiste el hash SHA-256, comparación en tiempo constante y nunca en logs (RN-25, RN-38, DD-06, KB 08 §Seguridad). El token de oferta usa "el mismo `ServicioToken` y la misma política" (KB 08).
- Estado actual del código (revisado en `master` al 2026-11-25):
  - No existe nada relacionado: no hay usos de `SecureRandom`, `MessageDigest`, `SHA-256`, `ServicioToken` ni `TokenGenerado` en `backend/src`. Los únicos "Token" son el CSRF de C-03 (`CsrfController`, `CookieCsrfTokenRepository`), el `AnonymousAuthenticationToken` de `SecurityConfig` y `JsonToken` de Jackson, que no tienen relación con esto.
  - `shared/` ya tiene `error`, `ratelimit`, `tiempo` y `web`. `shared.tiempo.Intervalo` es el antecedente de dominio puro: un `record` sin Spring que valida en el constructor compacto con `IllegalArgumentException`, y su test es JUnit 5 + AssertJ con `@DisplayName` en español.
  - `ArquitecturaTest` (C-01) tiene una sola regla ArchUnit, que cubre `..domain..` con `allowEmptyShould(true)`. **No cubre `shared.token`**, así que el criterio de salida "no importa Spring ni JPA (ArchUnit)" necesita una regla nueva.
  - ArchUnit 1.5.1 ya está en el `pom.xml`. Java 21.
- Consumidores (fichas de CHANGES.md):
  - C-32 `ServicioTokenTurno` emite (el token en claro se devuelve una sola vez), guarda `token_hash` (único, SHA-256 hex, KB 04 §TokenTurno) y valida.
  - C-36 `OfertaHueco` guarda el hash del token de la oferta y manda el enlace `APP_PUBLIC_URL/oferta/{token}`.
  - Los dos necesitan lo mismo: generar `{claro, hash}`, hashear un token que llega por la URL y comparar. Nada más.

## Goals / Non-Goals

**Goals:**
- Una API mínima, en español y sin estado mutable compartido, que cubra exactamente lo que piden C-32 y C-36.
- Que el uso incorrecto más probable (loguear el objeto con el token) sea inofensivo por construcción.
- Que todo se pruebe con tests unitarios rápidos y deterministas, sin Spring ni base de datos.

**Non-Goals:**
- Lo que la propuesta deja fuera de alcance (vigencia, revocación, persistencia, 404, endpoints, envío del email, captura de logs reales).
- Cualquier endurecimiento que la KB no pida: pepper o HMAC con clave, sal, rotación, versionado del formato o validación del formato base64url de lo que llega por la URL.

## API propuesta

Paquete `ar.edu.utn.turnos.shared.token` (dos tipos públicos, sin dependencias fuera del JDK):

```java
public final class ServicioToken {
    public ServicioToken();                                 // usa new SecureRandom()
    public TokenGenerado generar();                          // 32 bytes → base64url sin relleno (43) + su hash
    public String hashear(String claro);                     // SHA-256 sobre UTF-8 → 64 hex en minúscula; null/blank → IllegalArgumentException
    public boolean coincide(String claro, String hashGuardado); // tiempo constante; nunca lanza; inválidos → false
}

public record TokenGenerado(String claro, String hash) {
    // constructor compacto: rechaza null (IllegalArgumentException)
    @Override public String toString();                     // "TokenGenerado[claro=***, hash=***]"
}
```

Uso esperado por los consumidores (orientativo, lo deciden C-32 y C-36):
- **Emitir:** `var t = servicioToken.generar();` → se persiste `t.hash()` y se devuelve o envía `t.claro()` una sola vez.
- **Validar:** `servicioToken.hashear(claroDeLaUrl)` → se busca por `token_hash` (índice único); `coincide` queda disponible como comparación explícita en tiempo constante cuando el consumidor ya tiene el hash en la mano.

## Decisions

### D1. Ubicación en `shared.token` y no en `reservapublica`
- **Decisión:** `ar.edu.utn.turnos.shared.token`, como dice la ficha de C-14.
- **Por qué:** `listaespera` (C-36) también lo usa. Si viviera en `reservapublica`, `listaespera` dependería de otro módulo de feature, contra la regla de KB 08 de que los módulos solo comparten `shared`.
- **Alternativas:** dentro de `reservapublica` (lo que dice el árbol de KB 08): crea un acoplamiento entre features. Duplicarlo en cada módulo: dos implementaciones de criptografía para mantener, contra KB 08 ("el mismo `ServicioToken`").
- **Contradicción registrada:** KB 08 (árbol de paquetes) lo nombra en `reservapublica`. Prevalece la ficha de CHANGES.md, que ya lo justifica en "Riesgo o precondición". Este change no edita la KB (ver Open Questions).

### D2. Clase concreta sin interfaz ni bean de Spring
- **Decisión:** `ServicioToken` es una clase `final` con constructor público sin argumentos. No hay interfaz ni puerto, y no se registra como bean en este change.
- **Por qué:** el paquete no puede importar Spring (criterio de salida). Hay una sola implementación posible, porque la política es fija. Los consumidores son servicios de Spring y pueden hacer `new ServicioToken()` o declarar un `@Bean` en su propia configuración.
- **Alternativas:** una interfaz `GeneradorToken` con implementación: es abstracción sin un segundo caso. Un `@Component` en el paquete: rompe la regla ArchUnit. Una configuración `@Configuration` en otro paquete de `shared`: es agregar algo que ningún consumidor pidió todavía.

### D3. `new SecureRandom()` y no `SecureRandom.getInstanceStrong()`
- **Decisión:** una instancia de `new SecureRandom()` por servicio, compartida entre hilos (`SecureRandom` es thread-safe).
- **Por qué:** el algoritmo por defecto del JDK 21 (DRBG, o `NativePRNG` no bloqueante en Linux y `Windows-PRNG` en Windows) es criptográficamente seguro y suficiente para 256 bits. `getInstanceStrong()` puede usar una fuente **bloqueante** (`/dev/random` o `NativePRNGBlocking` en Linux) y colgar un request público o un test cuando hay poca entropía, sin aportar seguridad práctica para este caso.
- **Alternativas:** `getInstanceStrong()`, que tiene riesgo de bloqueo. `UUID.randomUUID()`: son 122 bits con formato reconocible, y DD-06 ya lo descartó. `Random` o `ThreadLocalRandom`: no son criptográficos.

### D4. base64url sin relleno
- **Decisión:** `Base64.getUrlEncoder().withoutPadding()` sobre 32 bytes, lo que da exactamente 43 caracteres de `[A-Za-z0-9_-]`.
- **Por qué:** es lo que pide RN-25 (43 caracteres). Viaja en la ruta `/oferta/{token}` sin escapes, porque no tiene `+`, `/` ni `=`.
- **Alternativas:** hexadecimal, que da 64 caracteres y un enlace más largo, contra RN-25. Base64 estándar: tiene `+`, `/` y `=`, que hay que escapar en la URL.

### D5. SHA-256 sin sal ni pepper, en hexadecimal minúscula
- **Decisión:** `MessageDigest.getInstance("SHA-256")` sobre los bytes UTF-8 del token, codificado con `HexFormat.of()` (64 caracteres en minúscula). Se crea un `MessageDigest` por llamada, porque no es thread-safe. La `NoSuchAlgorithmException` se envuelve en `IllegalStateException`, ya que todo JDK está obligado a incluir SHA-256.
- **Por qué:** la sal y los hashes lentos (BCrypt, Argon2) existen para proteger secretos de **baja entropía**, como las contraseñas, contra diccionarios. Con 256 bits aleatorios, invertir el hash o precomputar tablas es inviable, así que la sal no aporta. Además, el hash debe ser **determinista** para poder buscar por `token_hash` único (KB 04), y una sal por fila lo impediría. DD-06 y RN-25 piden "solo el hash SHA-256" y no mencionan sal: no hay contradicción. Se usa hex porque KB 04 dice "SHA-256 hex" y la ficha también.
- **Alternativas:** BCrypt o Argon2, que son lentos sin necesidad y no deterministas, así que no sirven para buscar. HMAC-SHA256 con pepper en `.env`: protegería si se filtrara la base **y** el token fuera débil, que no es el caso. Agregaría un secreto y configuración que la KB no pide (principio de apegarse al plan).

### D6. Comparación en tiempo constante con `MessageDigest.isEqual`
- **Decisión:** `coincide(claro, hashGuardado)`:
  1. Si `claro` es nulo o en blanco, o `hashGuardado` es nulo o su longitud no es 64, devuelve `false`.
  2. Si no, calcula `hashear(claro)` y devuelve `MessageDigest.isEqual(calculado.getBytes(US_ASCII), hashGuardado.getBytes(US_ASCII))`.
- **Por qué:** `MessageDigest.isEqual` es la primitiva del JDK documentada como de tiempo constante respecto del contenido. `String.equals` corta en el primer carácter distinto y filtraría por tiempo cuántos caracteres coinciden. El corte temprano por longitud o nulo no filtra nada secreto, porque la longitud del hash es pública y fija. Además, se comparan **hashes** y no tokens, así que lo que el atacante controla (el token) no se alinea con lo que se compara.
- **Nunca lanza:** así el consumidor mapea cualquier entrada inválida al mismo camino de "no coincide", que es el 404 uniforme de RN-25, sin un `try/catch` que se pueda olvidar.
- **Alternativas:** comparar los bytes decodificados del hex: es equivalente, pero necesita decodificar y manejar un hex inválido. Un bucle XOR propio: reinventa la primitiva del JDK.
- **Nota para C-32:** buscar por `token_hash` en un índice de PostgreSQL no es de tiempo constante, pero eso no filtra nada útil, porque el atacante elige el token, no el hash, y no puede dirigir prefijos del hash sin invertir SHA-256. Es el patrón estándar y no requiere más acá.

### D7. `TokenGenerado` como `record` con `toString` totalmente enmascarado
- **Decisión:** `toString()` devuelve `TokenGenerado[claro=***, hash=***]`. El constructor compacto rechaza `null` con `IllegalArgumentException`, igual que `Intervalo`.
- **Por qué:** el `toString` por defecto de un `record` imprime todos los componentes, así que un `log.info("{}", token)` filtraría el token. Se oculta también el hash: no es el secreto, pero no hace falta en un log y así no se correlacionan logs con filas de la base. La regla es simple: el objeto nunca imprime nada sensible.
- **Alternativas:** mostrar un prefijo del hash para depurar: es útil pero opcional, y la ficha no lo pide. Una clase con `char[]` que se borra: es una complejidad que no protege contra nada en este modelo, porque el valor ya es un `String` en el email.
- **Límite conocido:** el enmascarado no evita que alguien loguee `t.claro()` a mano ni que serialice el record con Jackson. Eso lo cubren los tests de captura de logs de C-32 y C-36 y la regla de no exponer `TokenGenerado` como DTO.

### D8. Validación de entrada mínima
- **Decisión:** `hashear` rechaza `null` y en blanco con `IllegalArgumentException` y un mensaje fijo ("El token no puede estar vacío"), que **nunca** incluye el valor recibido. No se valida que la entrada tenga formato base64url ni 43 caracteres.
- **Por qué:** hashear texto vacío es un error de programación del consumidor. Validar el formato es una conveniencia que la ficha no pide: un token mal formado simplemente no coincide con ningún hash y termina en el 404 uniforme.

### D9. Regla ArchUnit para `..shared.token..`
- **Decisión:** se agrega a `ArquitecturaTest` una segunda regla, separada de la de C-01:
  ```java
  @ArchTest
  static final ArchRule el_servicio_de_token_no_depende_de_frameworks = noClasses()
          .that().resideInAPackage("..shared.token..")
          .should().dependOnClassesThat().resideInAnyPackage(
                  "org.springframework..", "jakarta.persistence..", "org.hibernate..")
          .as("..shared.token.. no depende de Spring, JPA ni Hibernate");
  ```
  Usa los mismos paquetes prohibidos que la regla de C-01 y **no** lleva `allowEmptyShould(true)`: si el paquete se moviera o se vaciara, la regla falla en lugar de pasar sin verificar nada. Eso además da el RED natural antes de crear el paquete.
- **Alternativas:** ampliar la regla existente a `resideInAnyPackage("..domain..", "..shared.token..")`: mezcla un paquete que puede estar vacío con uno que no, y cambia la descripción de una regla de C-01. Marcar todo `shared` como dominio puro: es falso, porque `shared.web`, `shared.ratelimit` y `shared.tiempo.ConfiguracionReloj` usan Spring.

### D10. Cómo se prueba "tiempo constante" sin tests de tiempo
- **Decisión:** **no** se mide tiempo. Medir diferencias de nanosegundos en CI da tests inestables y no prueba nada con fiabilidad. Se prueba en dos partes:
  1. **Comportamiento:** coincide con su hash, no coincide con otro hash de la misma longitud, y los casos nulo, vacío y de otra longitud dan `false` sin lanzar.
  2. **Estructura:** un test con ArchUnit (`ClassFileImporter` sobre `ServicioToken`) verifica que la clase **llama** a `MessageDigest.isEqual(byte[], byte[])` y **no llama** a `String.equals(Object)`. Así la garantía queda delegada a la primitiva del JDK y una regresión a `equals` rompe el build.
- **Alternativas:** un microbenchmark con JMH, que es lento, ruidoso y fuera de alcance. Revisarlo solo en code review: no deja una verificación automática.

### D11. Tests de unicidad y alfabeto
- **10.000 muestras** ("miles", como dice la ficha): con 256 bits, la probabilidad de colisión es despreciable (del orden de 10⁻⁶⁹), así que una colisión indica un defecto real, como una semilla fija o pocos bytes. Tarda bastante menos de un segundo. Se verifica la unicidad del valor en claro y del hash.
- **Alfabeto:** regex `^[A-Za-z0-9_-]{43}$`, más la decodificación con `Base64.getUrlDecoder()` a 32 bytes, que prueba que hay 256 bits de material y no solo 43 caracteres.
- **Hash:** el vector conocido de SHA-256 (`abc` → `ba7816bf…15ad`, FIPS 180-2) prueba que el algoritmo es SHA-256, que la salida es hex en minúscula y que la codificación de entrada es correcta.
- Todos los tests llevan `@DisplayName` con el nombre exacto del escenario (regla dura 11).

## Modelo de amenazas

**Qué protege el token:** el acceso sin login a un turno (ver, confirmar, cancelar; RN-07) y a una oferta de hueco (ver, tomar; RN-38). Quien tiene el token puede **cancelar el turno de otro** o **quedarse con un hueco ofrecido a otro**. No da acceso a fichas, a la agenda ni a otros turnos.

| Amenaza | Qué podría hacer un atacante | Mitigación en C-14 | Mitigación fuera de C-14 |
|---|---|---|---|
| Adivinar o enumerar tokens | Probar valores hasta acertar uno válido | 256 bits de `SecureRandom`: unas 2²⁵⁶ posibilidades, inviable | Rate limit por IP en `/api/public/**` (C-17); 404 uniforme (C-32) |
| Token predecible | Deducir el próximo token a partir de uno visto | CSPRNG (`SecureRandom`), nunca `Random` ni UUID; test de unicidad | — |
| Filtración de la base de datos | Leer `token_hash` y usarlo como token | Solo se guarda el hash; con 256 bits no hay forma práctica de invertirlo | C-32 y C-36 persisten solo `hash()` |
| Filtración por logs | Leer el token en archivos de log | `toString` enmascarado de `TokenGenerado`; los errores no incluyen el valor | Tests de captura de logs (C-32, C-36); logs sin tokens (KB 08) |
| Ataque de tiempo en la comparación | Medir cuánto tarda la respuesta para descubrir el hash carácter por carácter | `MessageDigest.isEqual` sobre hashes; test estructural | 404 uniforme (C-32) |
| Reutilización entre turnos u ofertas | Usar el token del turno A en el turno B | Cada `generar()` es independiente y único | Alcance por turno (RN-07) y lookup por hash (C-32) |
| Token vigente después de usarse o cancelarse | Volver a usar un enlace viejo | Fuera de alcance | Vencimiento y revocación (C-32, C-36) |
| Intercepción en tránsito (email, URL) | Leer el enlace del email o del historial | Fuera de alcance (riesgo aceptado en DD-06) | HTTPS en despliegue; vencimiento al inicio del turno |

## Risks / Trade-offs

- [Un consumidor loguea `t.claro()` o serializa `TokenGenerado` a JSON] → El enmascarado solo cubre `toString`. Mitigación: los tests de captura de logs de C-32 y C-36 y la regla de no exponer `TokenGenerado` en la API (regla dura 9: DTOs). Se deja anotado en el Javadoc.
- [`SecureRandom` por defecto lento al arrancar en contenedores Linux con poca entropía] → El JDK 21 usa fuentes no bloqueantes para `new SecureRandom()`. Por eso se descarta `getInstanceStrong()` (D3).
- [El test de unicidad es probabilístico] → Con 256 bits, un falso positivo es despreciable. Si falla, es un defecto real.
- [El test estructural depende del nombre de la primitiva] → Si en el futuro se cambia por otra primitiva de tiempo constante, hay que actualizar el test a la vez. Es aceptable, porque es exactamente la verificación buscada.
- [Sin pepper, una filtración de la base **y** un token débil serían explotables] → El token nunca es débil (256 bits). La KB no pide pepper. Se acepta.
- [La KB 08 (árbol de paquetes) dice `reservapublica`] → Hay que dejar registrado que prevalece la ficha de CHANGES.md (D1).

## Migration Plan

No aplica: es código nuevo, sin datos, sin esquema y sin configuración. Para deshacerlo basta con revertir el PR. Ningún consumidor existe todavía.

## Open Questions

Ninguna bloquea, salvo la aprobación del governance CRITICAL, que es un gate y no una duda técnica.

1. **(No bloqueante) ¿Quién registra `ServicioToken` como bean?** Recomendación: lo decide C-32 (el primer consumidor), con un `@Bean` en su configuración o con `new ServicioToken()`. Si C-36 también lo necesita como bean, conviene moverlo a una configuración común en ese momento. Acá no se agrega nada.
2. **(No bloqueante) ¿Hay que actualizar el árbol de KB 08 (`reservapublica/ … ServicioToken`) a `shared/token`?** Recomendación: no tocar la KB en este change (principio de apegarse al plan). Si Juan Pablo lo pide al aprobar, se hace como un cambio de una línea en el mismo PR o al archivar.
3. **(No bloqueante) ¿Mostrar un prefijo del hash en `toString` para depurar?** Recomendación: no, se enmascara todo (D7). Se puede revisar si C-32 o C-36 lo necesitan.

## UI Mockups

- Design system revision: no aplica.
- Decision: no (no UI) — 2026-11-25. Es un servicio de dominio del backend, sin pantallas ni componentes.
- Mockups: ninguno.
- Approval: no aplica.
- Preview: no aplica.
- Visual verify: no aplica.
