# Spec Delta

## Purpose

Garantiza que cualquier integrante del equipo instale, compile, pruebe y levante la SPA desde cero de forma reproducible. La SPA consume el design system aprobado y habla con el backend desde el mismo origen, así las cookies de sesión funcionan sin abrir CORS (KB 08 §Seguridad, DD-03).

## ADDED Requirements

### Requirement: Instalación, build y tests reproducibles
El frontend SHALL instalarse desde el lockfile versionado sin resolver versiones nuevas. Sobre esa instalación limpia, la compilación de producción y la suite de tests MUST terminar en verde sin depender de un `.env` local ni de que el backend esté corriendo.

#### Scenario: La instalación limpia, el build y los tests terminan en verde
- **GIVEN** un clon limpio del repositorio, sin `node_modules/`, sin `dist/` y sin `.env`
- **WHEN** se ejecuta `npm ci && npm run build && npm test` en `frontend/`
- **THEN** los tres comandos terminan con código de salida 0
- **AND** `npm test` corre la suite una vez, sin modo watch

### Requirement: Chequeo estático estricto
El código del frontend SHALL compilar con el chequeo estricto de tipos activado. El análisis estático MUST rechazar el uso explícito del tipo `any` (regla dura 12).

#### Scenario: La configuración de TypeScript está en modo estricto
- **WHEN** se inspecciona la configuración efectiva de TypeScript del código de `src/`
- **THEN** la opción `strict` está activada

#### Scenario: El lint rechaza el uso explícito de any
- **GIVEN** un archivo TypeScript del frontend que declara una variable de tipo `any`
- **WHEN** se ejecuta el análisis estático del proyecto sobre ese archivo
- **THEN** el análisis informa un error por el uso de `any`

#### Scenario: El lint pasa sin errores sobre el código del frontend
- **WHEN** se ejecuta `npm run lint` en `frontend/`
- **THEN** el comando termina con código de salida 0

### Requirement: Nombre del producto tomado de la marca del design system
La SPA SHALL mostrar el nombre del producto que expone `BRAND` (definido en `design-system/brand.js`). Ese nombre MUST NOT aparecer repetido en el código del frontend, para que renombrar el producto implique editar un único lugar (design-system §Reemplazos exactos).

#### Scenario: La App muestra el nombre de la marca tomado de BRAND
- **GIVEN** la marca del design system cargada
- **WHEN** se renderiza la App
- **THEN** la pantalla muestra el texto de `BRAND.name`

#### Scenario: La App refleja un cambio del nombre en BRAND
- **GIVEN** `BRAND.name` con un nombre ficticio distinto del provisional
- **WHEN** se renderiza la App
- **THEN** la pantalla muestra ese nombre ficticio
- **AND** no muestra el nombre provisional

### Requirement: Tokens del design system aplicados
La SPA SHALL cargar `design-system/tokens.css` desde su ubicación en el repositorio, sin copiarlo. Los estilos globales MUST usar solo variables de ese archivo para color, fuente y espaciado, sin valores fijos (regla dura 13).

#### Scenario: Los estilos globales usan solo variables definidas en tokens.css
- **WHEN** se analizan los estilos globales del frontend
- **THEN** el fondo, el color de texto y la fuente de la página se definen con variables CSS
- **AND** cada variable usada está definida en `design-system/tokens.css`
- **AND** no hay colores hexadecimales, `rgb()` ni `hsl()` escritos de forma fija

#### Scenario: El build incluye los tokens del design system
- **GIVEN** `npm run build` ejecutado en `frontend/`
- **WHEN** se inspecciona la hoja de estilos generada en `dist/`
- **THEN** contiene la definición de `--color-primary` y de `--font-sans`

#### Scenario: El servidor de desarrollo sirve la SPA con los tokens aplicados
- **GIVEN** `npm run dev` corriendo en `frontend/`
- **WHEN** se abre la SPA en el navegador
- **THEN** la página muestra el nombre de `BRAND`
- **AND** el estilo calculado del `body` usa el fondo, el color de texto y la fuente de `tokens.css`
- **AND** la consola del navegador no muestra errores al cargar `tokens.css` ni `brand.js`

### Requirement: API en el mismo origen durante el desarrollo
En desarrollo, el servidor de la SPA SHALL reenviar al backend local (`http://localhost:8080`) toda petición cuyo path empiece con `/api`. El path original MUST conservarse, así la SPA y la API comparten origen y la cookie de sesión viaja sin CORS abierto.

#### Scenario: Las peticiones a /api se redirigen al backend en desarrollo
- **WHEN** se lee la configuración del servidor de desarrollo
- **THEN** el prefijo `/api` apunta a `http://localhost:8080`
- **AND** el path no se reescribe

#### Scenario: Una petición a /api desde el servidor de desarrollo llega al backend
- **GIVEN** el backend de C-01 corriendo en el puerto 8080 y `npm run dev` corriendo
- **WHEN** se pide `http://localhost:5173/api/inexistente`
- **THEN** responde el backend con su código de seguridad (401 o 403), no el `index.html` de la SPA

### Requirement: Base de la API configurable
La SPA SHALL tomar la URL base de la API de la variable `VITE_API_BASE_URL`. Si la variable no está definida, MUST usar `/api`, el valor de KB 08 §Variables de entorno.

#### Scenario: La base de la API es /api por defecto
- **GIVEN** `VITE_API_BASE_URL` no está definida
- **WHEN** la SPA resuelve la base de la API
- **THEN** la base es `/api`

#### Scenario: La base de la API toma el valor de VITE_API_BASE_URL
- **GIVEN** `VITE_API_BASE_URL` definida con `/api-prueba`
- **WHEN** la SPA resuelve la base de la API
- **THEN** la base es `/api-prueba`

### Requirement: Estructura de carpetas por feature
El código fuente del frontend SHALL organizarse en `src/app`, `src/shared` y `src/features`. `src/features` MUST tener una carpeta por cada feature de KB 08 §Estructura de directorios. Así cada change de front trabaja en su propia carpeta y no se pisa con los demás.

#### Scenario: La estructura de carpetas sigue la arquitectura del frontend
- **WHEN** se inspecciona `frontend/src`
- **THEN** existen las carpetas `app`, `shared` y `features`
- **AND** dentro de `features` existen `auth`, `agenda`, `pacientes`, `profesionales`, `prestaciones`, `lista-espera`, `configuracion`, `notificaciones` y `reserva-publica`
