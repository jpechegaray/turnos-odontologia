# Proposal

## Why

C-27 ya libera huecos y publica `HuecoLiberado`, pero nadie sabe todavía qué entradas de la lista de espera sirven para un hueco. C-31 define esa compatibilidad (RN-22) y la selección de a quién avisar (RN-23, SU-14). Lo necesita C-36, el aviso automático, que está en el camino crítico (C-27 → C-36 → C-42). También lo necesitan C-43 (asignación manual) y C-47 (frontend). Sus dependencias, C-18 y C-19, ya están en `master`.

## What Changes

- **`CompatibilidadListaEspera`:** una función pura del dominio, sin Spring ni base de datos. Decide si una entrada `ACTIVA` es compatible con un hueco `[inicio, fin)` de un profesional P (RN-22). Tienen que cumplirse todas estas condiciones:
  1. P realiza la prestación de la entrada y la prestación está activa;
  2. si la entrada tiene profesional preferido, es P;
  3. la duración por defecto de la prestación cabe en el hueco (`duracion <= fin − inicio`; 30 minutos en un hueco de 30 cabe);
  4. la fecha del inicio del hueco, en Buenos Aires, está dentro de `[fechaDesde, fechaHasta]`;
  5. si la entrada tiene franja, el turno resultante `[inicio, inicio + duración)` cae dentro de ella.
- **`SelectorDeAvisos`:** una función pura que elige a quién avisar entre las entradas compatibles (SU-14):
  - ordena por antigüedad (`creadoEn` ascendente y, si empatan, por `id`);
  - deja una sola entrada por paciente, la más antigua;
  - corta en `lista_espera.max_avisos_por_hueco`;
  - devuelve una lista vacía si el hueco ya empezó.
- **`BuscadorCompatibles`:** un componente de aplicación sin chequeo de rol, para que C-36 lo use desde un listener sin sesión. Trae de la base las entradas candidatas (`ACTIVA`, del profesional o sin preferido, con la fecha dentro del rango) y las filtra con la función pura.
- **`GET /api/lista-espera/compatibles?profesionalId&inicio&fin`**, para `RECEPCION` y `ADMIN`:
  - devuelve todas las entradas compatibles, ordenadas por antigüedad, sin tope ni deduplicación: son sugerencias, y decide recepción;
  - responde `400` si faltan parámetros o si `fin <= inicio`, y `422` si el profesional no existe o está inactivo;
  - un hueco que ya empezó devuelve una lista vacía;
  - `ODONTOLOGO` recibe `403`.
- **Quedan fuera:**
  - verificar que el hueco esté libre y dentro del horario de atención (RN-01, RN-02, RN-04): eso lo hace la creación del turno (C-36, C-43) con sus validaciones y las restricciones `EXCLUDE`;
  - la oferta, su token y el email (C-36);
  - la asignación manual (C-43) y el frontend (C-47).

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `lista-espera`: se agregan la compatibilidad entre un hueco y una entrada, la selección de entradas a avisar y la consulta de entradas compatibles.

## Impact

- **`listaespera.domain`:** nuevos `Hueco`, `CompatibilidadListaEspera` y `SelectorDeAvisos`.
- **`listaespera.application`:**
  - nuevo `BuscadorCompatibles`;
  - `ServicioListaEspera` gana `compatibles(profesionalId, inicio, fin)` con `@PreAuthorize`.
- **`listaespera.infrastructure`:** una consulta JPQL de candidatas por profesional y fecha.
- **`listaespera.web`:** `GET /api/lista-espera/compatibles`, que reutiliza `RespuestaEntrada`.
- **`SecurityConfig`:** matcher de `GET /api/lista-espera/compatibles` para `ADMIN` y `RECEPCION`.
- Se consumen sin cambios:
  - `ConsultaPrestaciones`, `ProfesionalRealizaPrestacion` y `ProfesionalConsulta`;
  - `ConfiguracionServicio.maxAvisosPorHueco()`;
  - `Clock`.
