# Cierre del MVP (C-51)

Este documento registra la verificación final del MVP. Incluye la búsqueda de secretos en el repositorio y el checklist contra el alcance que fijó el discovery (§D.3): qué entró al MVP, con su evidencia, y qué quedó para etapas posteriores.

Fecha de la verificación: 2026-11-26, en la rama `juan-pablo-c-51`.

## Búsqueda de secretos

Se revisaron solo los archivos versionados (`git ls-files`), sin abrir el `.env` local.

| Búsqueda | Comando | Resultado |
|---|---|---|
| Archivos sensibles por nombre | `git ls-files \| grep -iE '(^\|/)\.env($\|\.)\|\.pem$\|\.key$\|\.p12$\|\.jks$\|id_rsa\|credencial\|secret'` | Solo aparece `.env.example`, que es la plantilla prevista por la regla dura 2. No se abrió, por regla del equipo. |
| Claves privadas | `git grep -n 'BEGIN .*PRIVATE KEY'` | Sin resultados. |
| Formatos de token conocidos | `git grep -nE 'ghp_[A-Za-z0-9]{20,}\|AKIA[0-9A-Z]{16}\|xox[bap]-\|sk-[A-Za-z0-9]{20,}\|AIza[0-9A-Za-z_-]{30,}'` | Sin resultados. |
| Asignaciones de secretos | `git grep -niE '(password\|secret\|token\|api[_-]?key)\s*[:=]\s*["'"'"'][^"'"'"'$]{6,}'` | Dos falsos positivos: el campo `passwordHash` de `Usuario.java` y una clase CSS. Ninguno es un secreto. |

Conclusión: no hay secretos ni credenciales versionados. Las claves de los usuarios del seed y del SMTP se leen de variables de entorno (`.env`, ignorado por git).

## Checklist contra el alcance del MVP (§D.3)

### Imprescindibles

| Funcionalidad | Evidencia (spec / test / pantalla) |
|---|---|
| Agenda por profesional y por sillón, sin solapamientos | Specs `agenda-turnos`, `consulta-agenda` y `agenda-frontend`. La restricción `EXCLUDE` se prueba en los tests de `agenda/`, y la vista está en la pantalla Agenda. |
| Duración del turno según la prestación | Specs `catalogo-prestaciones` y `profesional-prestaciones`; pantalla Prestaciones. |
| Horarios de atención y bloqueos | Specs `horarios-de-atencion`, `bloqueos-de-agenda`, `horarios-frontend` y `bloqueos-frontend`. |
| Crear, reprogramar y cancelar turnos (plazo de 24 h configurable, cancelación tardía) | Specs `agenda-turnos` y `configuracion-parametros`; recorridos de `recorridos/RecorridoTurnoIT.java`. |
| Reserva online sin login y gestión del turno con token | Specs `reserva-publica`, `token-turno`, `token-seguro`, `reserva-publica-frontend` y `gestion-turno-token-frontend`. Lo cubre el escenario "Un paciente reserva online y gestiona su turno por el enlace" de `RecorridoTurnoIT`. |
| Ficha básica del paciente | Specs `gestion-pacientes` y `pacientes-frontend`. |
| Recordatorio por email (SMTP) | Specs `recordatorio-turno`, `notificaciones-email` y `registro-notificaciones`. Lo cubre el escenario "El recordatorio llega una sola vez y no llega al turno cancelado". |
| Roles (recepción, odontólogo de solo lectura, administración) | Specs `control-acceso` y `administracion-usuarios`. La matriz completa de permisos se prueba en `seguridad/web/MatrizPermisosIT.java`. |

### Diferenciador

| Funcionalidad | Evidencia |
|---|---|
| Lista de espera con aviso y toma del hueco liberado | Specs `lista-espera`, `oferta-hueco`, `toma-oferta-hueco`, `asignacion-hueco`, `lista-espera-frontend` y `oferta-hueco-frontend`. Recorridos "Una cancelación termina con el hueco tomado desde la lista de espera" y "Una reprogramación termina con el hueco tomado desde la lista de espera", más `AvisoHuecoReprogramacionIT`. |

### Etapas posteriores (no incorporadas)

Se buscaron en `backend/src` y `frontend/src` con `git grep -niwE` sobre cada término. La única coincidencia es "padrón", en un comentario de `useCatalogosListaEspera.ts` que habla del padrón de pacientes del consultorio; no es una validación de obras sociales.

| Funcionalidad | Términos buscados | Estado |
|---|---|---|
| Odontograma e historia clínica | `odontograma`, `historia clínica` | No incorporado |
| WhatsApp | `whatsapp` | No incorporado |
| Mercado Pago y señas | `mercadopago`, `mercado pago`, `seña` | No incorporado |
| Facturación ARCA | `factura`, `ARCA` (palabra completa) | No incorporado |
| Validación online de obras sociales | `padrón`, `validación de obra social` | No incorporado (la obra social es un dato de texto en la ficha) |
| Multisede | `multisede`, `sede` | No incorporado |
| Campañas | `campaña` | No incorporado |
| Asistentes con IA | `openai`, `llm`, `asistente` | No incorporado |

## Trazabilidad y reglas de arquitectura

- Cada una de las 38 reglas de negocio tiene al menos un test con el nombre exacto de su escenario: [matriz-rn-tests.md](matriz-rn-tests.md). Para comprobarlo, corré `node scripts/verificar-matriz-rn.mjs`.
- Las reglas de dependencia (el dominio no depende de frameworks y ningún módulo usa entidades ni repositorios de otro) corren en `ArquitecturaTest` y rompen la build si se violan.

## Pendiente manual

Falta seguir el [recorrido de demostración del README](../README.md#recorrido-de-demostración-c-51) sobre una base vacía. Necesita el `.env` local de cada integrante y no se automatizó: queda para que lo haga una persona del equipo antes de la entrega.
