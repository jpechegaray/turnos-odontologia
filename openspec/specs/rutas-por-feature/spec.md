# rutas-por-feature Specification

## Purpose
Permite que cada feature de la SPA sume sus pantallas y entradas de menú declarándolas en su propio `features/<feature>/routes.ts`, sin editar un router ni un menú central, y define el layout de navegación común donde se muestran.

## Requirements

### Requirement: Registro automático de rutas por feature
La SPA SHALL incorporar al router todas las rutas que declare cada archivo `features/*/routes.ts`, sin que haga falta editar archivos centrales. Cada ruta MUST tener una ruta única. Dos features que declaren la misma ruta MUST provocar un error explícito al construir el registro.

#### Scenario: Un routes.ts de ejemplo aparece en el router sin editar archivos centrales
- **GIVEN** una feature de ejemplo que declara la ruta `/ejemplo` en su `routes.ts`
- **WHEN** se construye el registro a partir de los `routes.ts` encontrados
- **THEN** el router incluye `/ejemplo` y navegar a esa ruta muestra la pantalla de la feature

#### Scenario: Dos features con la misma ruta producen un error explícito
- **GIVEN** dos features que declaran la ruta `/repetida`
- **WHEN** se construye el registro
- **THEN** la construcción falla con un error que menciona `/repetida`

### Requirement: Menú construido desde las features
Las rutas que declaren una entrada de menú SHALL aparecer en la navegación con su etiqueta, ordenadas por su orden numérico y, a igual orden, por etiqueta. Las rutas sin entrada de menú MUST seguir siendo navegables sin aparecer en el menú.

#### Scenario: Un routes.ts de ejemplo aparece en el menú sin editar archivos centrales
- **GIVEN** una feature de ejemplo con la entrada de menú "Ejemplo"
- **WHEN** se muestra el layout
- **THEN** la navegación incluye un enlace "Ejemplo" hacia `/ejemplo`

#### Scenario: El menú respeta el orden declarado
- **GIVEN** entradas "Agenda" con orden 10 e "Inicio" con orden 0
- **WHEN** se construye el menú
- **THEN** "Inicio" aparece antes que "Agenda"

#### Scenario: Una ruta sin entrada de menú no aparece en la navegación
- **GIVEN** una feature con una ruta sin entrada de menú
- **WHEN** se construye el menú
- **THEN** la ruta no figura en la navegación pero el router la incluye

### Requirement: Layout con navegación
Todas las pantallas del personal SHALL mostrarse dentro de un layout con un encabezado que muestre el nombre de la marca tomado de `BRAND`, una navegación (`nav`) con las entradas del menú, que marque como actual la de la ruta abierta, y el contenido de la pantalla.

#### Scenario: El layout muestra la marca, la navegación y la pantalla actual
- **GIVEN** la SPA abierta en la ruta de inicio
- **WHEN** se renderiza
- **THEN** se ve el nombre de la marca, una navegación con el enlace "Inicio" marcado como página actual y el contenido de inicio

### Requirement: Ruta inexistente
Una ruta que ninguna feature declara SHALL mostrar dentro del layout un mensaje de página no encontrada con un enlace para volver al inicio.

#### Scenario: Una ruta desconocida muestra página no encontrada
- **WHEN** el usuario abre `/ruta-que-no-existe`
- **THEN** ve el mensaje de página no encontrada y un enlace al inicio
