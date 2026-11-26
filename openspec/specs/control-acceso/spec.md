# control-acceso Specification

## Purpose
Define la política base de acceso a la API. Todo está denegado por defecto salvo la zona pública, y las respuestas de acceso denegado tienen formato uniforme. Los módulos declaran sus permisos por rol sobre esta base, y el login del personal (C-03) la extiende sin cambiar su comportamiento.

## Requirements

### Requirement: Denegación por defecto
Toda ruta de la API que no esté declarada como pública SHALL requerir un usuario autenticado. Dentro de `/api/auth/**`, las únicas excepciones de autenticación SHALL ser `GET /api/auth/csrf` y `POST /api/auth/login`; este último sigue sujeto a CSRF. Una petición anónima a una ruta protegida MUST recibir 401 en formato Problem Details, exista o no la ruta.

#### Scenario: Una petición anónima a una ruta protegida recibe 401
- **GIVEN** un cliente sin sesión
- **WHEN** envía `GET /api/turnos`
- **THEN** la respuesta tiene estado 401 y formato Problem Details

#### Scenario: Una ruta protegida inexistente también recibe 401
- **GIVEN** un cliente sin sesión
- **WHEN** envía `GET /api/ruta-que-no-existe`
- **THEN** la respuesta tiene estado 401 y no 404

#### Scenario: Consultar la sesión sin autenticar devuelve 401
- **WHEN** un cliente sin sesión envía `GET /api/auth/me`
- **THEN** recibe 401 en formato Problem Details

### Requirement: Zona pública abierta
Las rutas bajo `/api/public/**` SHALL ser accesibles sin autenticación ni sesión, también para métodos de escritura, porque las usa la reserva online del paciente.

#### Scenario: Una ruta pública responde sin autenticación
- **GIVEN** un cliente sin sesión y una ruta pública existente bajo `/api/public/`
- **WHEN** el cliente la invoca con GET
- **THEN** la respuesta no es 401 ni 403

#### Scenario: Una escritura pública no exige token CSRF
- **GIVEN** un cliente sin sesión y sin token CSRF
- **WHEN** envía un POST a una ruta pública existente bajo `/api/public/`
- **THEN** la respuesta no es 401 ni 403

### Requirement: Permisos por rol con formato uniforme
La API SHALL permitir que cada operación declare los roles que la pueden ejecutar. Un usuario autenticado sin el rol requerido MUST recibir 403 en formato Problem Details (base de RN-33).

#### Scenario: Un usuario sin el rol requerido recibe 403 Problem Details
- **GIVEN** un usuario autenticado con rol `ODONTOLOGO` y una operación reservada al rol `RECEPCION`
- **WHEN** el usuario invoca esa operación
- **THEN** la respuesta tiene estado 403 y formato Problem Details

#### Scenario: Un usuario con el rol requerido accede a la operación
- **GIVEN** un usuario autenticado con rol `RECEPCION` y una operación reservada al rol `RECEPCION`
- **WHEN** el usuario invoca esa operación
- **THEN** la operación se ejecuta y la respuesta es 200

### Requirement: Inicio y cierre de sesión del personal
La API SHALL aceptar en `POST /api/auth/login` un email y una contraseña para un usuario activo con rol `ODONTOLOGO`, `RECEPCION` o `ADMIN`. El email SHALL identificarse sin distinguir mayúsculas y minúsculas y ser único en su forma normalizada. Login y `GET /api/auth/me` SHALL devolver `200` con `{id, email, rol}` y sin credenciales. La sesión SHALL ser de servidor, con cookie `HttpOnly`, `SameSite=Lax` y `Secure` según `APP_COOKIE_SECURE` (habilitado por defecto fuera de desarrollo). `POST /api/auth/logout` SHALL invalidar la sesión y responder `204`. La API SHALL responder con el mismo `401` Problem Details para credenciales incorrectas, cuenta inexistente, inactiva o bloqueada, sin distinguir la causa. No SHALL registrar contraseñas, hashes, cookies ni valores CSRF.

#### Scenario: Un usuario activo inicia sesión y consulta su identidad
- **GIVEN** un usuario activo, credenciales válidas y un token CSRF válido
- **WHEN** inicia sesión con otra capitalización de su email y consulta `/api/auth/me` con la cookie recibida
- **THEN** ambas respuestas son 200 e incluyen solo su identidad y rol, y la cookie de sesión cumple los atributos configurados

#### Scenario: El inicio de sesión no reutiliza una sesión anterior
- **GIVEN** un cliente con un identificador de sesión previo
- **WHEN** inicia sesión correctamente
- **THEN** la sesión autenticada usa un identificador distinto

#### Scenario: El rechazo de login no revela el estado de la cuenta
- **GIVEN** peticiones con CSRF válido y datos con formato válido
- **WHEN** se intenta ingresar con una cuenta inexistente, una contraseña incorrecta, una cuenta inactiva o una cuenta bloqueada
- **THEN** todos los intentos reciben el mismo estado 401 y el mismo detalle público y ninguno crea una sesión autenticada

#### Scenario: Cerrar sesión invalida el acceso posterior
- **GIVEN** una sesión autenticada y un token CSRF válido
- **WHEN** el usuario envía `POST /api/auth/logout` y luego consulta `/api/auth/me` con la cookie anterior
- **THEN** logout responde 204 y la consulta posterior responde 401

#### Scenario: Los registros no contienen credenciales ni cookies
- **WHEN** se ejecutan intentos de login exitosos y fallidos y se cierra la sesión
- **THEN** los registros no contienen contraseñas, hashes, cookies de sesión ni valores CSRF

### Requirement: Bloqueo temporal por intentos fallidos
La API SHALL bloquear una cuenta activa durante 15 minutos a partir del quinto fallo consecutivo de contraseña. Durante el bloqueo SHALL rechazar incluso credenciales válidas sin extender el plazo. Al expirar el bloqueo SHALL iniciar una nueva serie; un ingreso exitoso también SHALL reiniciar el contador.

#### Scenario: El quinto fallo bloquea hasta el vencimiento exacto
- **GIVEN** cuatro fallos consecutivos de una cuenta activa
- **WHEN** ocurre el quinto fallo en el instante T
- **THEN** las credenciales válidas se rechazan antes de T + 15 minutos y vuelven a admitirse exactamente en T + 15 minutos

#### Scenario: Un ingreso correcto reinicia los intentos consecutivos
- **GIVEN** cuatro fallos consecutivos de una cuenta activa
- **WHEN** el usuario ingresa correctamente y después falla una vez
- **THEN** el fallo inicia una nueva serie y la cuenta no queda bloqueada

#### Scenario: Después del vencimiento se inicia una nueva serie
- **GIVEN** una cuenta cuyo bloqueo ya venció
- **WHEN** el primer intento posterior falla
- **THEN** el siguiente intento válido todavía puede iniciar sesión

### Requirement: Protección CSRF y origen permitido
`GET /api/auth/csrf` SHALL permitir obtener la cookie `XSRF-TOKEN` sin autenticar ni crear una sesión autenticada. El cliente SHALL enviar el valor en `X-XSRF-TOKEN` al hacer login, logout y escrituras protegidas. Un token faltante o inválido SHALL resultar en 403 Problem Details para esas operaciones cuando no corresponda un 401 por falta de autenticación. La SPA SHALL poder obtener un token vigente nuevamente después de login y logout. La API MUST conservar la exención de CSRF para `/api/public/**` y permitir CORS con credenciales solo para el origen explícito de `APP_CORS_ORIGIN`.

#### Scenario: La SPA obtiene CSRF sin sesión y lo renueva al cambiar de sesión
- **WHEN** el cliente consulta `GET /api/auth/csrf` antes del login y vuelve a consultarlo después de login y de logout
- **THEN** recibe la cookie CSRF utilizable en cada etapa y el endpoint no le concede autenticación

#### Scenario: Una operación que requiere CSRF rechaza tokens faltantes o inválidos
- **WHEN** el cliente intenta login sin CSRF válido, o con sesión intenta logout o una escritura protegida sin CSRF válido
- **THEN** recibe 403 Problem Details y la operación no se ejecuta

#### Scenario: Solo el origen configurado puede usar CORS con credenciales
- **WHEN** un cliente envía una solicitud CORS desde el origen configurado y otro la envía desde un origen distinto
- **THEN** solo el origen configurado recibe autorización CORS con credenciales

### Requirement: Alta inicial de administrador en desarrollo
El sistema SHALL crear el administrador inicial únicamente con el perfil `dev` y `SEED_ADMIN_PASSWORD` configurada. MUST guardar solo su hash BCrypt, no registrar el secreto y no modificar ni duplicar una cuenta inicial existente.

#### Scenario: El administrador de desarrollo se inicializa una sola vez
- **GIVEN** el perfil `dev` y un secreto inicial configurado
- **WHEN** se arranca dos veces con la misma base
- **THEN** existe un solo administrador inicial y el segundo arranque no modifica su contraseña

#### Scenario: El administrador inicial no se crea fuera de desarrollo
- **WHEN** se arranca sin el perfil `dev`, aunque esté configurado el secreto
- **THEN** el sistema no crea la cuenta inicial

### Requirement: Sesión vigente solo con el usuario habilitado
Cada request con sesión del personal SHALL verificar que el usuario siga existiendo, activo y con el mismo rol y email que tenía al iniciar sesión. Si no, la sesión MUST invalidarse y el request MUST tratarse como anónimo (401 en rutas protegidas). El usuario tiene que volver a iniciar sesión. El vínculo con el profesional no se guarda en la sesión: se resuelve en cada consulta.

#### Scenario: La sesión de un usuario desactivado deja de ser válida
- **GIVEN** un usuario con sesión iniciada que consulta `GET /api/auth/me` con éxito
- **WHEN** el administrador lo desactiva y el usuario repite el request con la misma cookie de sesión
- **THEN** la respuesta tiene estado 401 y esa sesión ya no sirve aunque el usuario sea reactivado después

#### Scenario: La sesión de un usuario al que se le cambió el rol deja de ser válida
- **GIVEN** un usuario `RECEPCION` con sesión iniciada
- **WHEN** el administrador le cambia el rol a `ADMIN` y el usuario repite un request con la misma sesión
- **THEN** la respuesta tiene estado 401 y, tras iniciar sesión de nuevo, `GET /api/auth/me` informa el rol `ADMIN`

#### Scenario: La sesión de un usuario sin cambios sigue vigente
- **GIVEN** un usuario con sesión iniciada
- **WHEN** el administrador modifica solo el nombre de ese usuario y el usuario repite un request
- **THEN** la respuesta no es 401

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
