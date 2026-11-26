# Spec Delta

## ADDED Requirements

### Requirement: Matriz de permisos por rol

La API SHALL aplicar en el backend la matriz de permisos de la base de conocimiento (KB 03, RBAC) para cada ruta del personal y cada rol. Una operación no permitida MUST responder `403` en Problem Details sin cambiar datos, y una sin sesión `401`. La verificación SHALL recorrer todas las rutas registradas, así una ruta nueva sin permiso declarado hace fallar la build.

#### Scenario: Recepción no administra usuarios ni modifica la configuración
- **GIVEN** un usuario con rol `RECEPCION` y CSRF válido
- **WHEN** consulta o modifica `/api/usuarios`, o modifica `/api/configuracion`
- **THEN** recibe `403` en Problem Details y no cambia ningún dato

#### Scenario: Recepción no modifica los catálogos
- **GIVEN** un usuario con rol `RECEPCION` y CSRF válido
- **WHEN** da de alta, modifica o da de baja un profesional, un sillón o una prestación
- **THEN** recibe `403` en Problem Details y no cambia ningún dato

#### Scenario: El odontólogo no lee usuarios, configuración ni el registro de notificaciones
- **GIVEN** un usuario con rol `ODONTOLOGO`
- **WHEN** consulta `/api/usuarios`, `/api/configuracion` o `/api/notificaciones`
- **THEN** recibe `403` en Problem Details sin datos

#### Scenario: Cada rol lee lo que la matriz le permite
- **GIVEN** un usuario de cada rol
- **WHEN** consulta la agenda, los catálogos y la lista de espera
- **THEN** recibe una respuesta distinta de `401` y de `403`

#### Scenario: Toda ruta del personal sin sesión responde 401
- **WHEN** un pedido sin sesión llega a cualquier ruta del personal registrada, salvo login, logout y CSRF
- **THEN** recibe `401` en Problem Details sin datos

#### Scenario: Una ruta sin fila en la matriz hace fallar la verificación
- **GIVEN** una ruta del personal registrada que no figura en la matriz de la prueba
- **WHEN** se ejecuta la prueba de la matriz de permisos
- **THEN** la prueba falla e indica la ruta faltante
