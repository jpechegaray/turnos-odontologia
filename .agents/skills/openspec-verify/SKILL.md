---
name: openspec-verify
description: Verifica que un change de OpenSpec implementado cumple sus specs, design y tasks antes de archivarlo. Mapea cada escenario (WHEN/THEN) de las delta specs a un test que lo cubre, corre los tests del backend (mvn verify) y del front (vitest), revisa tasks.md y las reglas de negocio de la KB, y deja un informe verify.md con veredicto. Usala cuando el usuario diga "verificar el change", "openspec verify", "opsx verify", "¿está listo para archivar?", o después de /opsx:apply y antes de /opsx:archive. No implementa código ni corrige fallas: solo verifica e informa.
allowed-tools: Bash(openspec:*), Bash(mvn:*), Bash(./mvnw:*), Bash(npm:*), Bash(npx:*), Bash(git:*), Read, Grep, Glob, Write
license: MIT
metadata:
  author: equipo turnos-odontologia
  version: "1.0"
---

# openspec-verify — verificación de un change contra sus specs

Paso entre `/opsx:apply` y `/opsx:archive`. Contesta una sola pregunta, con evidencia:
**¿lo implementado hace lo que dicen las specs del change, y está probado?**

Regla de oro: **no se afirma nada que no se haya ejecutado o leído.** Si un test no se corrió, el informe dice "no ejecutado", nunca "pasa".

## Entrada

- Nombre del change. Si no se da, correr `openspec list --json`; si hay más de uno activo, preguntar cuál.
- `openspec status --change "<name>" --json` para confirmar que existen `proposal.md`, `design.md`, `tasks.md` y `specs/`.

## Pasos

### 1. Validación estructural
```bash
openspec validate "<name>" --strict
```
Si falla, se informa como **bloqueante** y se sigue con el resto (para dar el panorama completo).

### 2. Tareas
Leer `openspec/changes/<name>/tasks.md`. Toda tarea sin `[x]` es **bloqueante**, salvo que el `design.md` la marque explícitamente como diferida.

### 3. Matriz escenario → test (el corazón de la skill)
Para cada `#### Scenario:` de `openspec/changes/<name>/specs/**/spec.md`:

1. Extraer el nombre y sus pasos WHEN/THEN.
2. Buscar el test que lo cubre:
   - Backend: `src/test/java/**` (JUnit 5). Primero por `@DisplayName` que contenga el nombre del escenario; después por el nombre del método o por palabras clave del THEN.
   - Front: `**/*.test.ts(x)` (Vitest), con `describe`/`it`.
3. Leer el test y confirmar que **afirma el THEN**: un test que solo llama al endpoint sin chequear el resultado no cubre.
4. Clasificar: **Cubierto** (archivo:línea) / **Parcial** (cubre una parte del THEN, decir qué falta) / **Sin test**.

Convención del equipo para que el mapeo sea directo: `@DisplayName("<nombre exacto del escenario>")` en JUnit y el mismo texto en el `it(...)` de Vitest.

### 4. Ejecutar los tests
- Backend (si hay `pom.xml`): `./mvnw -q verify` (o `mvn -q verify`). Testcontainers necesita Docker andando; si no está, se informa "no ejecutado — Docker apagado". **No se marca como pasado.**
- Front (si hay `package.json` con vitest): `npx vitest run`.
- Registrar: comando, tests pasados/fallidos/saltados y el nombre de cada test fallido. Un escenario "Cubierto" cuyo test falla pasa a **Falla**.

### 5. Reglas de negocio y reglas duras
Cruzar el change con `knowledge-base/05_reglas_de_negocio.md`: si el proposal o las specs citan reglas RN-XX, verificar que haya escenario y test para cada una. Prestar atención especial a las reglas críticas del dominio:
- RN-01/RN-02: sin solapamientos, intervalos `[inicio, fin)`. Tiene que haber un test del caso de borde (un turno empieza justo cuando termina otro) y otro de solapamiento real.
- Concurrencia: dos reservas sobre el mismo hueco. Si el change toca la creación de turnos, se espera un test con la constraint `EXCLUDE` de PostgreSQL (Testcontainers), no solo la validación en Java.
- Token del paciente (RN-07): alcance y expiración.

Revisar también las reglas duras del proyecto en el diff del change (`git diff master...HEAD`):
- Sin datos reales de pacientes en seeds, fixtures o tests: solo datos ficticios.
- Sin secretos ni credenciales (`.env`, contraseñas o tokens en `application*.yml`).
Cualquier violación es **bloqueante**.

### 6. Diseño
Comparar el `design.md` con el código: paquetes y capas, decisiones explícitas y endpoints. Las diferencias se informan como **mayor** (contradice una decisión) o **menor** (detalle). Si el change tiene UI y `design.md` registra mockups aprobados, aplicar la verificación visual de la política del proyecto; si la política es `system-only`, solo chequear que se usen los tokens de `design-system/tokens.css`.

## Salida

Escribir `openspec/changes/<name>/verify.md`:

```markdown
# Verify — <name> (<fecha>)

**Veredicto:** LISTO PARA ARCHIVAR | NO LISTO (<n> bloqueantes)

## Validación estructural
## Tareas            (x/y completas; pendientes)
## Matriz escenario → test
| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
## Ejecución de tests (comando, resultado, fallidos)
## Reglas de negocio y reglas duras
## Diferencias con design.md  (bloqueante / mayor / menor)
## Qué NO se pudo verificar
## Próximo paso
```

Veredicto **LISTO** solo si no hay bloqueantes: validate OK, todas las tasks hechas, todos los escenarios Cubiertos, tests ejecutados y verdes, y ninguna regla dura violada. Los escenarios Parciales y las diferencias mayores se listan como "a revisar por Juan Pablo en el PR".

Al usuario se le muestra un resumen corto: el veredicto, los bloqueantes y la ruta a `verify.md`. Si el veredicto es LISTO, sugerir `/opsx:archive <name>`; si no, listar qué hay que arreglar (volver a `/opsx:apply`).

## Qué no hace

- No escribe ni corrige código ni tests.
- No archiva el change.
- No marca tareas como hechas.
