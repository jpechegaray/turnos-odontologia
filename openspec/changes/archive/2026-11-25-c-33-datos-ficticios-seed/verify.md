# Verify — c-33-datos-ficticios-seed (2026-11-25)

**Veredicto:** LISTO PARA REVISIÓN Y ARCHIVO TRAS EL MERGE.

## Validación estructural

`openspec validate c-33-datos-ficticios-seed --strict` aprobó la propuesta, diseño, delta y tareas.

## Tareas

4/4 completadas.

## Matriz escenario → test

Todos los escenarios corresponden a la capacidad `datos-demostracion`. Las líneas son relativas a `backend/src/test/java/ar/edu/utn/turnos/shared/seed/DevDataSeederIT.java`.

| Escenario | Estado | Test y resultado comprobado |
|---|---|---|
| El seed solo se habilita en desarrollo | Cubierto | L58. El contexto sin dev no registra el componente y el contexto dev sí. |
| Sin clave de personal no se cargan datos de demostración | Cubierto | L80. Las ocho tablas operativas siguen vacías. |
| Una clave de personal inválida no inicia la carga | Cubierto | L87. La carga rechaza la clave y no deja filas. |
| La base vacía queda lista para demostrar la agenda | Cubierto | L96. Verifica catálogos, horarios, pacientes, vínculos, BCrypt, administrador original y tres turnos, dos de ellos contiguos. |
| Repetir la carga en otro día no duplica los datos | Cubierto | L139. Se conservan conteos, hashes e inicios incluso al cambiar el día y la clave configurada. |
| Una base operativa existente se conserva | Cubierto | L155. Un sillón inactivo se conserva como única fila operativa. |
| Una falla durante la carga revierte los datos | Cubierto | L165. Una falla al crear turnos revierte las altas previas y restaura el contexto anterior. |

## Ejecución de tests

- **Rojo:** `mvnw.cmd -B -ntp -Dit.test=DevDataSeederIT verify` falló al compilar las pruebas porque faltaba el runner.
- **Verde dirigido:** `mvnw.cmd -B -ntp -Dit.test=DevDataSeederIT -Dtest=DevDataSeederIT verify` ejecutó las siete pruebas sobre PostgreSQL 16 de Testcontainers. Pasaron las siete tanto en Surefire como en Failsafe.
- **Suite backend:** `mvnw.cmd -B -ntp verify` aprobó 253 unitarios y 503 de integración de Failsafe. En total, 756 tests sin fallos, errores ni omitidos. JaCoCo cubrió 77 de 77 líneas de `shared.seed`.
- **Suite frontend:** `npm ci`, `npm run lint`, `npm test` y `npm run build` aprobaron. Vitest ejecutó 290 tests en 34 archivos y el build procesó 139 módulos. npm y React Router emitieron avisos ya presentes en la base.

## Reglas de negocio y reglas duras

- RN-35. Las cuentas del personal usan el dominio reservado `consultorio.test`. Los pacientes de prueba usan `example.test`, DNIs de la base de conocimiento y obra social ficticia. La clave se obtiene de la configuración local y se guarda mediante el servicio de usuarios con BCrypt. La plantilla deja vacía la clave del seed nuevo.
- RN-01, RN-02 y RN-13. Se reutiliza `CrearTurnoService` con los servicios de catálogo y las restricciones EXCLUDE existentes. El escenario de carga comprueba dos turnos contiguos dentro del horario y un turno con el segundo profesional. La fecha se calcula con `Clock` y la zona de Buenos Aires.
- La transacción y el bloqueo de inicialización evitan cargas parciales o duplicadas. El runner restaura el contexto de seguridad de su hilo en un `finally`. Se mantienen el perfil y el contrato del administrador existente.

## Diferencias con design.md

Ninguna. La presencia de datos operativos impide la recarga, aun cuando el catálogo solo contenga registros inactivos. Un reinicio conserva los datos y los horarios de la primera carga.

## Qué NO se pudo verificar

El arranque sobre una base local con el archivo `.env` del usuario quedó fuera de la prueba. La activación del componente se probó en un contexto Spring ligero. Las altas, las restricciones y la reversión se probaron sobre PostgreSQL 16 aislado con Testcontainers y credenciales de prueba.

## Próximo paso

Revisar el diff y solicitar la confirmación del commit. Abrir el PR después de la aprobación.
