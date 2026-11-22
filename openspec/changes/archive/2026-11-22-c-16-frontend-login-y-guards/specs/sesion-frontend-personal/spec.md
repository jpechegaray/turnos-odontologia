# Spec Delta

## Purpose

Define cómo el personal inicia, recupera y cierra su sesión en la SPA y cómo se limita la navegación de acuerdo con su rol.

## ADDED Requirements

### Requirement: Ingreso del personal
La SPA SHALL ofrecer `/login` con campos accesibles de email y contraseña y enviar el formulario a `POST /api/auth/login` con la protección CSRF de C-03. MUST impedir envíos duplicados mientras el pedido esté pendiente. Tras un `200` SHALL usar la identidad devuelta y navegar a Inicio (`/`); MUST NOT persistir la identidad ni la contraseña en almacenamiento web.

#### Scenario: El personal inicia sesión y llega a Inicio
- **WHEN** el personal envía credenciales válidas y la API devuelve su identidad
- **THEN** la SPA muestra Inicio con la sesión activa, sin persistir las credenciales ni la identidad en almacenamiento web

#### Scenario: Un envío pendiente no duplica el login
- **WHEN** la persona vuelve a intentar enviar el formulario mientras el primer pedido está pendiente
- **THEN** no se emite un segundo login

### Requirement: Mensajes de ingreso sin revelar el estado de la cuenta
Ante cualquier `401` del login, la SPA SHALL mostrar «No pudimos iniciar sesión. Verificá tus credenciales» y la misma ayuda «Si ya hiciste varios intentos, esperá unos minutos antes de volver a probar». MUST NOT inferir un bloqueo, mostrar un contador ni repetir el detalle del servidor. Un fallo distinto de `401` SHALL mostrar un mensaje de operación no completada, no un error de credenciales.

#### Scenario: Todos los rechazos de credenciales muestran el mismo mensaje
- **WHEN** el login devuelve `401` por contraseña incorrecta, cuenta inexistente, inactiva o bloqueada
- **THEN** el formulario permanece visible y muestra el mismo mensaje y ayuda en todos los casos

#### Scenario: Un fallo técnico de ingreso no se presenta como credenciales inválidas
- **WHEN** el login falla por red, devuelve `403` o un error de servidor
- **THEN** la pantalla informa que el ingreso no se pudo completar y permite reintentar sin mostrar el rechazo de credenciales

### Requirement: Restauración y expiración de sesión
Al entrar al área del personal, la SPA SHALL consultar `GET /api/auth/me` sin mostrar contenido interno mientras espera. Un `401` de esa consulta o de una operación interna SHALL dejar la sesión como anónima y reemplazar la navegación por `/login`; un `403` interno MUST NOT cerrar sesión. Un fallo técnico al restaurar SHALL mostrar un error recuperable con reintento, sin asumir que la sesión venció.

#### Scenario: Una recarga recupera la identidad desde el servidor
- **WHEN** el personal recarga una ruta permitida con sesión válida
- **THEN** el contenido interno espera la confirmación de `/api/auth/me` y se muestra en la misma ruta con el rol devuelto

#### Scenario: Entrar al área interna sin sesión lleva a login
- **WHEN** la consulta inicial de sesión devuelve `401`
- **THEN** la SPA reemplaza la navegación por `/login` sin mostrar contenido interno

#### Scenario: Una operación interna con sesión vencida lleva a login
- **WHEN** una operación interna para una sesión activa devuelve `401`
- **THEN** la SPA descarta la identidad en memoria y reemplaza la navegación por `/login`

#### Scenario: Un 403 interno no cierra la sesión
- **WHEN** una operación interna devuelve `403`
- **THEN** se conserva la sesión y no se redirige a login por ese error

#### Scenario: Un fallo técnico al restaurar sesión permite reintentar
- **WHEN** la consulta inicial falla por red o servidor
- **THEN** la SPA no muestra contenido interno ni redirige a login y permite volver a consultar la sesión

### Requirement: Cierre de sesión del personal
La SPA SHALL ofrecer «Cerrar sesión» en el layout interno y enviar `POST /api/auth/logout` con CSRF. Con `204` o `401` SHALL descartar la identidad y reemplazar la navegación por `/login`. Con otro error MUST informar que no pudo confirmar el cierre, mantener la sesión local y permitir reintentar.

#### Scenario: Cerrar sesión lleva a login y no deja volver al contenido interno
- **WHEN** se pide cerrar sesión y la API devuelve `204` o `401`
- **THEN** se elimina la identidad local, se navega a `/login` y volver atrás no muestra contenido protegido

#### Scenario: Un fallo de logout permite reintentar sin anunciar el cierre
- **WHEN** el logout falla por red o con un error distinto de `401`
- **THEN** la SPA mantiene la sesión local, informa el fallo y permite reintentar

### Requirement: Navegación según rol y separación del área pública
Las pantallas del personal SHALL exigir sesión y respetar los roles declarados; una ruta interna sin restricción adicional SHALL estar disponible a los tres roles del personal. El menú SHALL mantener el orden de C-09 y mostrar únicamente entradas internas permitidas al rol. `ODONTOLOGO` MUST NOT ver entradas ni controles operativos de escritura. Las rutas públicas de paciente, cuando se registren, MUST NOT exigir sesión del personal, compartir su menú ni redirigir a login por errores de la API pública. El backend conserva la autorización efectiva.

#### Scenario: El menú y el acceso directo respetan el mismo permiso
- **GIVEN** una ruta interna que permite `ADMIN` pero no `RECEPCION` ni `ODONTOLOGO`
- **WHEN** el personal abre el menú o intenta acceder directamente
- **THEN** solo `ADMIN` ve la entrada y puede abrirla; los otros roles ven «No tenés permiso» al acceder directamente, sin perder la sesión

#### Scenario: El odontólogo ve lectura pero no acciones operativas de escritura
- **WHEN** una persona con rol `ODONTOLOGO` navega por la zona interna
- **THEN** accede a las vistas de lectura habilitadas para su rol sin ver entradas ni controles operativos de escritura

#### Scenario: Una ruta pública no depende de la sesión del personal
- **WHEN** una persona sin sesión abre una ruta pública registrada y una llamada a la API pública responde `401`
- **THEN** la ruta no muestra el menú interno, no exige sesión del personal y no redirige a `/login`
