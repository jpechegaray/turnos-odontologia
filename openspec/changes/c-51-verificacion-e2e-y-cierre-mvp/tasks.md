# Tasks

## 1. Reglas de arquitectura

- [x] 1.1 Con TDD, agregar a `ArquitecturaTest` la regla «Módulos sin acceso a la persistencia ajena» (design, decisión 5), sin `allowEmptyShould`. Primero, comprobar que la regla falla con una clase de prueba que viola la regla, para el escenario «Usar el repositorio de otro módulo rompe la build»; después, retirar esa clase. Verificar con `mvnw test -Dtest=ArquitecturaTest`, que cubre «El código actual cumple las reglas de dependencia».
- [x] 1.2 Revisar que la regla existente del dominio no pase vacía (cubre «Una dependencia del dominio hacia el framework rompe la build»). Si hoy hay paquetes `domain`, quitar `allowEmptyShould` y verificar con la misma prueba. Si aparece una violación real, frenar e informar antes de tocar código de producción.

## 2. Matriz de permisos

- [x] 2.1 Escribir `MatrizPermisosIT` con la tabla de KB 03 §RBAC (método, ruta, rol y resultado esperado) y la comprobación de que cada ruta registrada del personal tiene fila. Verificar el escenario «Una ruta sin fila en la matriz hace fallar la verificación» con una fila quitada a propósito, y después restaurarla.
- [x] 2.2 Cubrir con la misma tabla «Recepción no administra usuarios ni modifica la configuración», «Recepción no modifica los catálogos», «El odontólogo no lee usuarios, configuración ni el registro de notificaciones» y «Cada rol lee lo que la matriz le permite». Lo denegado responde 403 sin cambios de filas. Verificar con `mvnw verify -Dit.test=MatrizPermisosIT`.
- [x] 2.3 Cubrir «Toda ruta del personal sin sesión responde 401». Si alguna fila falla por un permiso real, frenar (dominio CRITICAL): documentar el hallazgo y proponer el arreglo, sin escribirlo hasta tener aprobación.

## 3. Recorridos del turno

- [x] 3.1 Crear el paquete de prueba `recorridos` con la base común: reloj fijo, correo en memoria, datos ficticios propios, IP distinta por pedido público, lectura del token desde el cuerpo del email, sondeo acotado para el aviso asíncrono y limpieza por ids. Verificar que compila y que una prueba vacía de humo levanta el contexto compartido.
- [x] 3.2 Con TDD, escribir «Un paciente reserva online y gestiona su turno por el enlace»: reserva pública, agenda del personal, un solo email de confirmación, consulta y cancelación por el enlace.
- [x] 3.3 Con TDD, escribir «Una cancelación termina con el hueco tomado desde la lista de espera»: cancelación por recepción, aviso de cancelación, oferta asíncrona, toma con el token del email, confirmación del turno nuevo y entrada `ASIGNADA`.
- [x] 3.4 Con TDD, cubrir en `oferta-hueco` «Una reprogramación avisa por el horario que dejó libre» y «Una reprogramación que se superpone avisa solo por el tramo liberado». Escribir también el recorrido «Una reprogramación termina con el hueco tomado desde la lista de espera». Si el aviso por reprogramación no funciona, arreglarlo con el test en rojo primero (MEDIUM) y anotar la decisión en este design.
- [x] 3.5 Con TDD, escribir «El recordatorio llega una sola vez y no llega al turno cancelado» con dos corridas del planificador.
- [x] 3.6 Ejecutar la suite completa del backend con `mvnw verify` en un entorno limpio, sin base local. Así queda cubierto «Los recorridos pasan en una build limpia»; ningún email real se envía porque el correo es en memoria.

## 4. Trazabilidad y cierre

- [x] 4.1 Escribir `docs/matriz-rn-tests.md` con RN-01 a RN-38, spec y escenario, y archivo y nombre del test (backend y frontend). Agregar un script versionado que verifique las 38 filas, que ninguna tenga el test vacío y que cada nombre citado exista en los tests. Verificar «Cada regla tiene al menos un test que existe» corriendo el script.
- [x] 4.2 Si el script encuentra una RN sin test, escribir el test que falta con el nombre de su escenario, o, si la regla ya no aplica, documentarlo y consultarlo antes de cerrar la fila.
- [x] 4.3 Buscar secretos en los archivos versionados (`git ls-files` con patrones de `.env`, claves privadas, contraseñas y tokens) y registrar el comando y el resultado en `docs/cierre-mvp.md`. Cubre «La búsqueda de secretos no encuentra nada versionado».
- [x] 4.4 Completar `docs/cierre-mvp.md` con el checklist contra `informe-discovery.md` §D.3: cada función imprescindible y diferenciadora con su evidencia (spec, test o pantalla), y cada función de "Etapas posteriores" marcada como no incorporada. Cubre «El checklist cubre el MVP y excluye el backlog futuro».
- [x] 4.5 Actualizar el README con el arranque desde cero (`docker compose up`, `.env` desde `.env.example`, backend `dev` con seed, frontend) y un recorrido de demostración con los usuarios del seed, sin contraseñas. Verificar «Un integrante nuevo arranca la demo siguiendo el README» siguiendo los pasos en una base vacía. *(Esa corrida en base vacía queda manual: necesita el `.env` de cada integrante; ver `verify.md`.)*
- [x] 4.6 Actualizar `CHANGES.md` (C-51 hecho y el avance final) y correr `openspec validate --strict`, `mvnw verify` y, en el frontend, `npx vitest run`, `npx tsc -b` y `npx eslint src`. Dejar `verify.md` con el mapeo entre escenarios y tests.

## Workflow follow-up

- Abrir el PR a `master` desde `juan-pablo-c-51` con "Closes #N" y mergear solo con CI en verde.
- Archivar el change (`/opsx:archive`) después del merge, en un PR de archivo aparte.
