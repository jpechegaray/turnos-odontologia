## ADDED Requirements

### Requirement: Acciones del encabezado de la lista de espera

La pantalla SHALL mostrar «Anotar paciente» y «Asignar hueco» en el encabezado. Cada botón SHALL alternar la visibilidad de su formulario, indicar su estado con `aria-expanded` y enfocar el panel al abrirlo. La pantalla MUST mostrar como máximo un formulario a la vez y conservar los datos cargados al alternar.

#### Scenario: Los botones de lista de espera abren su formulario
- **GIVEN** los dos formularios están ocultos al entrar
- **WHEN** se selecciona «Anotar paciente» y después «Asignar hueco»
- **THEN** se muestra solamente el formulario de la acción seleccionada y un segundo clic en ella vuelve a ocultarlo

## MODIFIED Requirements

### Requirement: Listado de entradas por estado

La SPA SHALL consultar las entradas de todos los estados y mostrar los conteos y el filtro de estado. La vista inicial SHALL mostrar las entradas activas. Cada fila SHALL mostrar el nombre del paciente, la prestación, el profesional («Cualquiera» si no hay preferencia), el rango de fechas, la franja y el estado. Si un catálogo no responde, SHALL mostrar un nombre de respaldo con el número de ID.

#### Scenario: El listado muestra las entradas activas con nombres
- **WHEN** recepción abre la pantalla
- **THEN** la SPA consulta las entradas y muestra las activas con paciente, prestación, profesional, rango, franja y estado con nombres legibles

#### Scenario: Cambiar el filtro de estado consulta ese estado
- **WHEN** recepción elige otro estado en el filtro
- **THEN** la SPA muestra solamente las entradas de ese estado

#### Scenario: Un estado sin entradas lo informa
- **WHEN** la vista de un estado no contiene entradas
- **THEN** la pantalla informa que no hay entradas en ese estado
