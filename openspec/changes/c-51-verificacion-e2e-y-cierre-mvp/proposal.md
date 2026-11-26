# Proposal

## Why

Los 50 changes anteriores probaron cada módulo por separado, pero el MVP no tiene todavía una prueba que recorra el turno de punta a punta cruzando módulos, ni una evidencia única de que cada regla de negocio (RN-01 a RN-38) y cada fila de la matriz de permisos estén cubiertas. C-51 es la hoja del roadmap: cierra el MVP comprobando que todo funciona junto en un entorno limpio y dejando la documentación para la demostración y la entrega.

Lo que ya existe y se reutiliza: `ArquitecturaTest` (el `domain` no depende de Spring ni JPA), `EscriturasOdontologoIT` (toda escritura del odontólogo responde 403), y pruebas por módulo de recordatorio idempotente, avisos por cancelación y reprogramación, ofertas y toma del hueco. Lo que falta:

- **Flujo por reprogramación hacia la lista de espera:** C-35 publica `HuecoLiberado` al reprogramar, y la lista de espera lo escucha, pero ninguna prueba ni ninguna spec cubre ese camino (CHANGES.md, riesgo de C-35 y RM-06). La spec `oferta-hueco` solo nombra la cancelación.
- **Recorrido cruzado:** reservar online, cancelar, avisar a la lista, tomar el hueco y recibir los emails, todo por HTTP sobre un mismo contexto y una misma base.
- **Matriz de permisos completa:** hoy solo se prueba la fila "odontólogo no escribe". Faltan, por ejemplo, recepción sin acceso a usuarios ni a escribir la configuración, y el odontólogo sin acceso a usuarios, configuración y registro de notificaciones.
- **Regla de módulos:** un módulo no usa entidades JPA ni repositorios de otro (KB 08); hoy no hay un test que lo impida.
- **Trazabilidad y cierre:** la matriz RN→test, el README con arranque y demo, la comprobación de que no hay secretos versionados y el checklist contra el MVP de `informe-discovery.md` §D.3.

## What Changes

- Nuevas pruebas de integración de recorrido (Testcontainers + MockMvc) que cruzan `reservapublica`, `agenda`, `listaespera` y `shared/notificacion`, incluido el hueco liberado por una reprogramación.
- Nueva prueba de la matriz de permisos de KB 03 §RBAC por rol (`ODONTOLOGO`, `RECEPCION`, `ADMIN`) y sin sesión, con las rutas tomadas del registro de Spring MVC.
- Nueva regla ArchUnit: ninguna clase de un módulo depende de entidades JPA ni de repositorios de otro módulo.
- `docs/matriz-rn-tests.md`: cada RN-01 a RN-38 con el escenario de spec y el test que la cubre; ninguna fila queda vacía.
- `docs/cierre-mvp.md`: checklist contra §D.3 (imprescindible y diferenciador cubiertos; nada de "Etapas posteriores" incorporado) y resultado de la búsqueda de secretos.
- README: arranque desde cero con `docker compose up`, seed y recorrido de demostración.
- Actualización de `CHANGES.md` con el avance final.
- No se toca `frontend/src` ni el código de producción salvo que una prueba destape un defecto; ver Impact.

## Capabilities

### New Capabilities
- `recorrido-turno`: comportamiento de punta a punta del turno entre módulos (reserva online, cancelación y reprogramación, aviso a la lista de espera, toma del hueco y emails), verificado en un entorno limpio.
- `reglas-arquitectura`: reglas de dependencia que el código debe cumplir y que se verifican automáticamente (dominio sin frameworks, módulos sin acceso a entidades ni repositorios ajenos).
- `trazabilidad-reglas`: la matriz que vincula cada regla de negocio con su escenario y su test, y el checklist de cierre del MVP.

### Modified Capabilities
- `oferta-hueco`: el aviso automático también se dispara por los tramos que libera una reprogramación confirmada, no solo por una cancelación.
- `control-acceso`: se agrega el requisito de la matriz de permisos completa por rol de KB 03 §RBAC.

## Impact

- **Código:** tests nuevos en `backend/src/test`, `docs/`, `README.md`, `CHANGES.md` y los artefactos de este change. Sin dependencias nuevas: ArchUnit, Testcontainers y spring-security-test ya están en el `pom.xml`.
- **Si una prueba falla por un defecto real:** se documenta y se frena. Un hueco en permisos es dominio CRITICAL: solo se analiza y se propone el arreglo, que requiere aprobación explícita antes de escribir código. Un defecto de negocio (MEDIUM) se corrige con su test en este mismo change, mostrando la decisión.
- **Fuera de alcance:** pruebas de punta a punta del frontend con navegador, cambios de diseño y cualquier función de "Etapas posteriores".
- **Riesgo:** las pruebas de recorrido usan el reloj inyectable y el adaptador de correo en memoria; no envían emails reales ni dependen de la hora del sistema.
