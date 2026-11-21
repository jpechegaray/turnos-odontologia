# Cobertura de escenarios

Los escenarios de la [delta](specs/catalogo-sillones/spec.md) tienen un `@DisplayName` idéntico al título de la primera columna.

| Escenario | Prueba |
|---|---|
| Los tres roles consultan sillones activos e inactivos | `SillonesControllerTest#consultaCatalogoYDetalle` |
| Consultar un ID inexistente devuelve 404 | `SillonesControllerTest#detalleInexistente` |
| El administrador da de alta un sillón activo | `SillonesApiIT#daDeAlta` |
| El administrador edita un sillón sin cambiar su identidad | `SillonesApiIT#edita` |
| El nombre duplicado devuelve 409 incluso si el otro sillón está inactivo | `SillonesApiIT#impideDuplicados` |
| Dos altas concurrentes del mismo nombre no crean duplicados | `SillonesApiIT#soloUnaAltaConcurrentePersiste` |
| Una escritura con campos inválidos devuelve 400 | `SillonesApiIT#validaCampos` |
| Actualizar un ID inexistente devuelve 404 | `SillonesApiIT#noCreaAlEditarAusente` |
| Desactivar un sillón conserva su registro | `SillonesApiIT#desactiva` |
| Reactivar un sillón conserva su registro | `SillonesApiIT#reactiva` |
| Recepción y odontología no pueden escribir en el catálogo | `SillonesApiIT#rolesDeLecturaNoEscriben` |
| Un cliente sin sesión no puede acceder al catálogo | `SillonesApiIT#requiereSesion` |
| Un administrador sin CSRF válido no puede escribir en el catálogo | `SillonesApiIT#adminRequiereCsrf` |
| Agenda deja de ofrecer un sillón desactivado | `SillonConsultaIT#excluyeElDesactivado` |
| Agenda vuelve a ofrecer un sillón reactivado | `SillonConsultaIT#incluyeElReactivado` |
| Agenda distingue un sillón inactivo de uno inexistente | `SillonConsultaIT#distingueEstadosDeAusencia` |

**Pruebas complementarias:** `SillonJpaIT` verifica la identidad y la restricción `uk_sillon_nombre`; `ServicioSillonesTest` verifica que no se traduzcan otras violaciones de integridad a `409`.

**Integración con C-03:** `SillonesSesionIT` comprueba las escrituras con sesión real de ADMIN, la cookie `XSRF-TOKEN` y la cabecera `X-XSRF-TOKEN`, así como los rechazos `403` con sesión y CSRF ausente o inválido, y `401` sin sesión ni CSRF. Estas pruebas se omiten explícitamente si el árbol solo incluye la seguridad base de C-01.

**Fuera de alcance:** la guarda de baja con turnos futuros activos queda en C-30; C-05 no implementa bloqueos por intervalo ni comprueba disponibilidad de turnos.

**Ejecución de referencia:** `.\mvnw.cmd -B clean verify` desde `backend/` en el árbol con C-03: 44 pruebas unitarias/web (incluido ArchUnit) y 69 de integración con PostgreSQL 16, sin fallos ni omisiones. El resultado no verifica el árbol aislado sobre C-01.
