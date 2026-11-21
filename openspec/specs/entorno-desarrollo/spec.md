# entorno-desarrollo Specification

## Purpose
Garantiza que cualquier integrante del equipo levante desde cero, con un solo comando, la base de datos y el backend en un entorno reproducible y con secretos fuera del repositorio. También garantiza que las pruebas de integración corran contra PostgreSQL real (US-021, RN-35).

## Requirements

### Requirement: Base de datos local con un solo comando
El repositorio SHALL incluir una definición de contenedores que, con `docker compose up -d` desde la raíz y sin pasos manuales previos (salvo copiar `.env.example` a `.env`), levante PostgreSQL 16 con un volumen de datos persistente y un chequeo de salud. La extensión `btree_gist` MUST estar disponible para instalarse en esa base.

#### Scenario: El entorno levanta PostgreSQL 16 desde cero
- **GIVEN** una máquina con Docker, sin contenedores ni volúmenes previos del proyecto y con `.env` copiado de `.env.example`
- **WHEN** se ejecuta `docker compose up -d` en la raíz del repositorio
- **THEN** el servicio de base de datos queda en estado saludable
- **AND** `SELECT version()` informa PostgreSQL 16

#### Scenario: La extensión btree_gist está disponible en la base local
- **GIVEN** la base levantada con `docker compose up -d`
- **WHEN** se consulta `pg_available_extensions`
- **THEN** la lista incluye `btree_gist`

### Requirement: Extensión btree_gist instalada al arrancar
Al arrancar, el backend SHALL dejar instalada la extensión `btree_gist` en la base configurada, ejecutando un script de restricciones idempotente después de que se genere el esquema. Arrancar dos veces seguidas sobre la misma base MUST NOT fallar.

#### Scenario: La extensión btree_gist queda instalada al arrancar la aplicación
- **GIVEN** una base PostgreSQL 16 vacía
- **WHEN** la aplicación arranca con el perfil `test`
- **THEN** `pg_extension` contiene `btree_gist`

#### Scenario: El script de restricciones es idempotente
- **GIVEN** una base en la que el script de restricciones ya se ejecutó una vez
- **WHEN** el script se ejecuta de nuevo sobre la misma base
- **THEN** la ejecución termina sin error
- **AND** `btree_gist` sigue instalada una sola vez

### Requirement: Secretos fuera del repositorio
Las credenciales y los parámetros sensibles SHALL leerse de variables de entorno o de un archivo `.env` local. El archivo `.env` MUST quedar excluido del control de versiones y SHALL versionarse solo `.env.example`, con todas las variables de la KB y valores de ejemplo que no sean secretos reales.

#### Scenario: El archivo .env no se versiona
- **GIVEN** un archivo `.env` en la raíz del repositorio
- **WHEN** se ejecuta `git check-ignore .env`
- **THEN** git informa que `.env` está ignorado
- **AND** `git ls-files` no lista ningún `.env` salvo `.env.example`

#### Scenario: El archivo .env.example declara todas las variables del entorno
- **WHEN** se revisa `.env.example`
- **THEN** contiene todas las variables de la tabla de KB 08 §Variables de entorno
- **AND** las variables sensibles tienen valores de ejemplo como `cambiar-en-env-local`, nunca credenciales reales

### Requirement: Configuración por perfiles
El backend SHALL ofrecer un perfil `dev`, que conserva el esquema entre arranques y agrega tablas y columnas nuevas, y un perfil `test`, que crea el esquema limpio en cada ejecución y lo descarta al terminar. Ningún perfil MUST contener credenciales escritas en el código.

#### Scenario: El perfil test arranca con un esquema limpio
- **GIVEN** el perfil `test` activo
- **WHEN** arranca el contexto de la aplicación
- **THEN** el modo de generación del esquema es de creación y descarte (`create-drop`)

#### Scenario: El perfil dev conserva el esquema entre arranques
- **GIVEN** el perfil `dev` activo
- **WHEN** se revisa la configuración efectiva del esquema
- **THEN** el modo de generación del esquema es de actualización (`update`)

### Requirement: Pruebas de integración contra PostgreSQL real
Las pruebas de integración SHALL ejecutarse contra un PostgreSQL 16 real que se levanta automáticamente durante la build, sin bases en memoria y sin depender de la base local de `docker compose` ni del archivo `.env`.

#### Scenario: El contexto de la aplicación carga con el perfil test sobre PostgreSQL real
- **GIVEN** Docker disponible y ningún `.env` presente
- **WHEN** se ejecuta la suite de integración
- **THEN** el contexto de la aplicación carga con el perfil `test`
- **AND** la base conectada informa PostgreSQL 16

#### Scenario: La build completa pasa en una máquina limpia
- **GIVEN** una máquina con JDK 21 y Docker, sin dependencias en caché
- **WHEN** se ejecuta `./mvnw verify` en `backend/`
- **THEN** la build termina con éxito, con todas las pruebas unitarias y de integración en verde
