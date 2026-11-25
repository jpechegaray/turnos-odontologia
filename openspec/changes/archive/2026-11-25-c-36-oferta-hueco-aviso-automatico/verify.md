# Verify — c-36-oferta-hueco-aviso-automatico (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR (después del merge del PR)

## Validación estructural

`openspec validate c-36-oferta-hueco-aviso-automatico --strict` → `Change 'c-36-oferta-hueco-aviso-automatico' is valid`.

## Tareas

23 de 23 completas. La 6.3 se cerró con este informe. La actualización de CHANGES.md se movió al commit de archivado, que es la convención del equipo (C-28, C-31 y C-33 actualizaron el roadmap al archivar, no en el PR del change).

## Matriz escenario → test

Spec: `specs/oferta-hueco/spec.md`. Rutas relativas a `backend/src/test/java/ar/edu/utn/turnos/listaespera/oferta/`.

| Requisito | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| Oferta de hueco | La base rechaza una segunda oferta para la misma entrada y el mismo hueco | Cubierto | `infrastructure/OfertaHuecoTablaIT.java:50` | Inserta por SQL; espera `DataIntegrityViolationException`. |
| Oferta de hueco | La base rechaza un hueco vacío o invertido | Cubierto | `infrastructure/OfertaHuecoTablaIT.java:59` | Prueba `hueco_fin = hueco_inicio`; el invertido cae en el mismo `CHECK`. |
| Oferta de hueco | La oferta nueva queda enviada y vence al inicio del hueco | Cubierto | `domain/OfertaHuecoTest.java:24` | También se ve en la fila real (`AvisoHuecoLiberadoIT.java:192`). |
| Token de la oferta | Solo se guarda el hash del token | Cubierto | `application/AvisoHuecoLiberadoIT.java:192` | Hash de 64 hexadecimales igual al SHA-256 del token del enlace; ninguna columna contiene el claro. |
| Token de la oferta | El token no aparece en los logs | Cubierto | `application/AvisoHuecoLiberadoIT.java:210` | `OutputCaptureExtension`, con 2 tokens enviados. |
| Aviso automático | Tras cancelar solo reciben aviso las entradas compatibles | Cubierto | `application/AvisoHuecoLiberadoIT.java:54` | Consulta de 30 minutos contra conducto de 90, por la API real. |
| Aviso automático | El tope de avisos elige a las entradas más antiguas | Cubierto | `application/AvisoHuecoLiberadoIT.java:68` | Tope 2, 3 entradas. |
| Aviso automático | Un paciente con dos entradas compatibles recibe un solo aviso | Cubierto | `application/AvisoHuecoLiberadoIT.java:85` | Además, la función pura está en `listaespera/domain/SelectorDeAvisosTest.java:42` (C-31). |
| Aviso automático | Sin entradas compatibles no se avisa a nadie | Cubierto | `application/AvisoHuecoLiberadoIT.java:101` | 0 ofertas y 0 intentos de envío. |
| Aviso automático | Una cancelación revertida no avisa | Cubierto | `application/AvisoHuecoLiberadoIT.java:112` | `TransactionTemplate` con `setRollbackOnly`; el turno sigue `PROGRAMADO`. |
| Aviso automático | Un fallo al avisar no hace fallar la cancelación | Cubierto | `application/AvisoHuecoFalloIT.java:55` | `OfertasHueco` reemplazado por un mock que lanza; responde 200 y el turno queda `CANCELADO`. |
| Una sola tanda | Liberar dos veces el mismo hueco no duplica ofertas | Cubierto | `application/AvisoHuecoLiberadoIT.java:131` | También verifica que una entrada nueva sí recibe su oferta. |
| Email de aviso | El email lleva el enlace con el token de la oferta | Cubierto | `application/AvisoHuecoLiberadoIT.java:148` | Asunto, fecha y hora en Buenos Aires, prestación, profesional y enlace que coincide con el hash. |
| Email de aviso | Un paciente sin email no genera oferta | Cubierto | `application/AvisoHuecoLiberadoIT.java:165` | |
| Email de aviso | El aviso queda registrado asociado a la oferta | Cubierto | `application/AvisoHuecoLiberadoIT.java:176` | Un solo registro `OFERTA_HUECO`, `ENVIADA`, sin turno y con `inicio_referencia` igual al inicio del hueco. |
| Reintento | El reintento envía un enlace nuevo y válido | Cubierto | `application/CicloOfertaHuecoIT.java:69` | Hash rotado; el token nuevo encuentra la oferta y el hash anterior ya no está. |
| Reintento | No se reintenta después de empezar el hueco | Cubierto | `application/CicloOfertaHuecoIT.java:87` | Con reloj fijo, se pone `inicio_referencia` en "ahora" en vez de mover el reloj. |
| Reintento | No se reintenta una oferta que ya no está enviada | Cubierto | `application/CicloOfertaHuecoIT.java:102` | Oferta `VENCIDA`: no hay intento y el registro queda terminal (dos corridas). |
| Vencimiento | Una oferta vence al inicio del hueco | Cubierto | `application/CicloOfertaHuecoIT.java:119` | Borde con `expira_en = ahora`. |
| Vencimiento | Una oferta futura sigue vigente | Cubierto | `application/CicloOfertaHuecoIT.java:129` | Un minuto en el futuro (la spec dice 09:59 contra 10:00). |
| Vencimiento | El vencimiento no toca ofertas tomadas y se puede repetir | Cubierto | `application/CicloOfertaHuecoIT.java:139` | |
| Búsqueda por token | Un token vigente encuentra su oferta | Cubierto | `application/CicloOfertaHuecoIT.java:154` | |
| Búsqueda por token | Tokens inválidos dan el mismo resultado | Cubierto | `application/CicloOfertaHuecoIT.java:164` | Inexistente, mal formado, nulo, vencido, tomado y expirado sin planificador. |

**Resultado:** 23 de 23 escenarios cubiertos, ninguno parcial.

C-36 no modifica código de C-15. Al integrar con master se usó el gancho `descartarSiFaltanDatos` de C-37 y se descartó el cambio provisorio en `ServicioNotificaciones` y su test.

## Ejecución de tests

- **Backend:** `./mvnw -o verify` (2026-11-25, con Docker andando), sobre `origin/master` 1e8c4c5 (incluye C-32, C-35 y C-37) más C-36 → EXIT 0, `BUILD SUCCESS`.
  - Surefire: 306 tests, 0 fallas, 0 errores, 0 saltados.
  - Failsafe: 633 tests, 0 fallas, 0 errores, 0 saltados.
  - Del change: `OfertaHuecoTest` 4, `OfertaHuecoTablaIT` 3, `AvisoHuecoLiberadoIT` 11, `AvisoHuecoFalloIT` 1 y `CicloOfertaHuecoIT` 8, todos en verde.
  - Antes de integrar con master, la suite sobre c6fb0fe había dado 290 unitarios y 573 IT en verde.
- **Front:** `npx vitest run` (2026-11-25 15:59; el change no toca `frontend/`) → 34 archivos y 290 tests, todos en verde.
- **Prueba manual en dev** (tarea 6.2): cancelación por la API real → 200 en unos 150 ms; 1 oferta `ENVIADA` y 1 aviso `OFERTA_HUECO` `ENVIADA`, creados en el hilo `aviso-hueco-1`; ningún enlace en los logs. Datos borrados al terminar.

## Reglas de negocio y reglas duras

- **RN-06** (cancelar ofrece el hueco): `AvisoHuecoLiberadoIT`, por la API real.
- **RN-22** (compatibilidad): "Tras cancelar solo reciben aviso las entradas compatibles"; la función pura está cubierta en C-31.
- **RN-23 / SU-14** (aviso a todos, tope por antigüedad, uno por paciente, una sola tanda): escenarios de tope, paciente doble y "Liberar dos veces".
- **RN-24** (si nadie confirma, las ofertas vencen al inicio del hueco): escenarios de vencimiento. La toma y "el primero gana" son de C-42.
- **RN-38** (token propio de 256 bits, solo el hash, nunca en logs, resultado uniforme, vigente hasta el inicio, reintentos cesan al empezar el hueco): escenarios de token, búsqueda y reintento.
- **RN-13** (no se avisa si el hueco ya empezó): lo garantiza `BuscadorCompatibles.aAvisar` / `SelectorDeAvisos` (C-31, con tests propios). No hay un IT de C-36 que cancele un turno ya empezado.
- **RN-01/02 y la concurrencia de creación de turnos:** no aplican; C-36 no crea turnos.
- **Reglas duras:**
  - Datos ficticios: los emails de los tests son `*@oferta.test` y los datos de la prueba manual fueron `*.c36@ejemplo.test`, ya borrados.
  - Secretos: no se versiona ninguno. `application.yml` usa `${APP_PUBLIC_URL:…}`. Los cambios locales de `.env.example` siguen en un stash y `docker-compose.yml` (puerto 5433) no entra en el commit.
  - Token hasheado (regla 3): cumple.
  - Sin endpoints nuevos (regla 4): no aplica.
  - `timestamptz` / `Instant` (regla 6): cumple.
  - Hibernate sin Flyway (regla 7): cumple.
  - DTO y JPQL parametrizado (regla 9): cumple.
  - Testcontainers sin H2 (regla 10): cumple.
  - `@DisplayName` con el nombre exacto del escenario (regla 11): cumple.

## Diferencias con design.md

- **Menor (D5):** el descarte del reintento usa `descartarSiFaltanDatos` (C-37) en lugar de cambiar `ServicioNotificaciones`; design.md ya está actualizado. Además, el reintento con rotación de token vive en `ReconstructorAvisoOferta` y no en `OfertasHueco`. Es para cortar el ciclo de beans `OfertasHueco → ServicioNotificaciones → reconstructores`. El armado del email se extrajo a `ContenidoAvisoOferta`. El comportamiento es el mismo.
- **Menor (D8):** el `CHECK (hueco_fin > hueco_inicio)` lo genera Hibernate con `@Check`, como en `lista_espera`; design.md ya está actualizado. En `schema-constraints.sql` van `fk_oferta_hueco_lista_espera` y `fk_oferta_hueco_profesional`.
- **Menor (D2), a revisar en el PR:** declarar el bean `TaskExecutor` `avisosHuecoExecutor` puede hacer que Spring Boot no autoconfigure su `applicationTaskExecutor`. Hoy no hay otros `@Async` ni MVC asíncrono, así que no tiene efecto, pero conviene tenerlo presente.
- **D3 (implementación):** el duplicado se atrapa fuera de la transacción `REQUIRES_NEW`. Atraparlo adentro la dejaba marcada para rollback y cortaba los avisos de las demás entradas del hueco. Lo cubre "Liberar dos veces el mismo hueco no duplica ofertas".

## Qué NO se pudo verificar

- **Envío por SMTP real:** la suite y la prueba manual usan el canal en memoria. El adaptador SMTP es de C-15 y no cambió.
- **Modo asincrónico en la suite:** los IT corren con `SyncTaskExecutor`. El hilo `aviso-hueco-1` solo se vio en la prueba manual.
- **El caso RN-13 por la API de cancelación**, ver arriba.

## Próximo paso

1. Commit en la rama de Valentino, con consulta previa.
2. Push y PR a `master`, que revisa Juan Pablo. Mencionar en el PR que, desde C-35, el aviso también corre al reprogramar (un `HuecoLiberado` por tramo liberado); el extremo a extremo de ese camino es C-51.
3. Después del merge, `/opsx:archive c-36-oferta-hueco-aviso-automatico`, que actualiza CHANGES.md (C-36 hecho, avance 31 de 51).
