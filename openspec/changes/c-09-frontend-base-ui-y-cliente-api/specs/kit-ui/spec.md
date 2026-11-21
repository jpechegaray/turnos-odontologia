# Spec Delta

## Purpose

Ofrece los componentes visuales comunes de la SPA (botón, campo, badge de estado de turno, tabla, toast, tarjeta y alerta) fieles al design system aprobado, accesibles y sin valores visuales fijos, para que todas las pantallas se vean y se comporten igual.

## ADDED Requirements

### Requirement: Estilos solo con tokens del design system
Los estilos del kit UI SHALL usar únicamente variables definidas en `design-system/tokens.css` para colores, espaciados, radios, sombras, tipografía y movimiento. MUST NOT contener colores fijos (hexadecimales, `rgb` o `hsl`) ni variables inexistentes.

#### Scenario: El kit UI no usa colores fuera de tokens.css
- **WHEN** se revisan las hojas de estilo del kit UI
- **THEN** no contienen colores fijos y toda variable usada está definida en `tokens.css`

### Requirement: Botón
El botón SHALL ofrecer las variantes primaria (por defecto), secundaria, peligro y peligro atenuado, un tamaño chico y ancho completo. Por defecto MUST tener `type="button"` para no enviar formularios por accidente, y deshabilitado MUST no ejecutar su acción.

#### Scenario: El botón ejecuta su acción y respeta la variante
- **GIVEN** un botón de variante peligro con el texto "Cancelar turno"
- **WHEN** el usuario lo presiona
- **THEN** se ejecuta su acción, tiene la clase de la variante peligro y `type="button"`

#### Scenario: Un botón deshabilitado no ejecuta su acción
- **GIVEN** un botón deshabilitado
- **WHEN** el usuario intenta presionarlo
- **THEN** la acción no se ejecuta

### Requirement: Campo de formulario accesible
El campo SHALL asociar su etiqueta al control. Con ayuda o error, MUST vincularlos al control con `aria-describedby`. Con error, MUST marcar el control con `aria-invalid="true"` y mostrar el mensaje con un ícono.

#### Scenario: El campo asocia la etiqueta y la ayuda al control
- **GIVEN** un campo con etiqueta "Nombre" y ayuda "Como figura en el catálogo"
- **WHEN** se renderiza
- **THEN** el control es accesible por la etiqueta "Nombre" y su descripción incluye la ayuda

#### Scenario: Un campo con error queda marcado como inválido
- **GIVEN** un campo con el error "La duración debe ser múltiplo de 5"
- **WHEN** se renderiza
- **THEN** el control tiene `aria-invalid="true"` y su descripción incluye el mensaje de error

### Requirement: Badge de estado de turno
El badge SHALL mostrar cada estado con color, ícono y texto: `PROGRAMADO` como "Reservado", `CONFIRMADO` "Confirmado", `ATENDIDO` "Atendido", `AUSENTE` "Ausente", `CANCELADO` "Cancelado" (tachado), cancelación tardía "Cancelación tardía" y bloqueo "Bloqueo". El ícono MUST ser decorativo, ya que el texto transmite el estado.

#### Scenario: Cada estado de turno se muestra con su texto y su estilo
- **WHEN** se renderiza un badge para cada estado
- **THEN** cada uno muestra su texto en español y su clase de estilo, con un ícono oculto para lectores de pantalla

#### Scenario: El estado cancelado se muestra tachado
- **WHEN** se renderiza un badge del estado `CANCELADO`
- **THEN** su texto lleva el estilo tachado del design system

### Requirement: Tabla
La tabla SHALL recibir columnas y filas, mostrar un título accesible (`caption`) y encabezados de columna, alinear a la derecha las columnas numéricas y mostrar un mensaje cuando no hay filas.

#### Scenario: La tabla muestra título, encabezados y filas
- **GIVEN** una tabla "Prestaciones" con columnas Nombre y Duración y dos filas
- **WHEN** se renderiza
- **THEN** la tabla es accesible por el nombre "Prestaciones", muestra los dos encabezados y las dos filas

#### Scenario: Una tabla sin filas muestra el mensaje de vacío
- **GIVEN** una tabla sin filas con el mensaje "No hay prestaciones"
- **WHEN** se renderiza
- **THEN** muestra "No hay prestaciones"

### Requirement: Toast, alerta y tarjeta
El toast SHALL anunciarse como estado (`role="status"`) y admitir una acción opcional. La alerta de peligro MUST anunciarse con `role="alert"`, y la informativa no interrumpe al lector de pantalla. La tarjeta SHALL agrupar contenido con un título opcional.

#### Scenario: El toast se anuncia y ejecuta su acción
- **GIVEN** un toast "Turno cancelado" con la acción "Deshacer"
- **WHEN** el usuario presiona "Deshacer"
- **THEN** el toast tiene `role="status"` y se ejecuta la acción

#### Scenario: Una alerta de peligro se anuncia de inmediato
- **WHEN** se renderiza una alerta de peligro con el título "No se pudo guardar"
- **THEN** tiene `role="alert"` y muestra el título

#### Scenario: La tarjeta muestra su título y contenido
- **WHEN** se renderiza una tarjeta con título "Próximo turno" y un contenido
- **THEN** muestra el título como encabezado y el contenido
