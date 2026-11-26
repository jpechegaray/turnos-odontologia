## ADDED Requirements

### Requirement: Menú lateral agrupado con íconos

En escritorio la navegación SHALL ser un menú lateral con el logo y la marca arriba, las entradas en grupos titulados (Atención, Consultorio, Administración) y un ícono decorativo por entrada. Las entradas que una feature declare sin grupo MUST aparecer igual, en el grupo Atención. Un grupo sin entradas visibles para el rol MUST NOT mostrarse.

#### Scenario: El menú agrupa las entradas por sección
- **GIVEN** una sesión de `ADMIN`
- **WHEN** se renderiza el layout
- **THEN** la navegación principal muestra los grupos Atención, Consultorio y Administración, con "Agenda" en Atención y "Usuarios" en Administración

#### Scenario: Un grupo sin entradas para el rol no se muestra
- **GIVEN** una sesión de `ODONTOLOGO`, que no ve ninguna entrada de Administración
- **WHEN** se renderiza el layout
- **THEN** la navegación no muestra el título "Administración"

### Requirement: Menú desplegable en celular

En pantallas angostas el layout SHALL ocultar el menú lateral y mostrar la barra superior de acciones y la navegación inferior de la propuesta. Los accesos a Inicio, Agenda y Pacientes SHALL respetar las entradas permitidas por rol. Los botones "Abrir menú" y "Más" SHALL abrir el menú completo e indicar su estado con `aria-expanded`. El menú MUST cerrarse con Escape o al elegir una entrada.

#### Scenario: El botón "Más" abre y cierra el menú completo
- **GIVEN** el layout renderizado
- **WHEN** el usuario elige "Más" y después presiona Escape
- **THEN** el menú completo se abre con `aria-expanded="true"` y al presionar Escape se cierra con `aria-expanded="false"`
