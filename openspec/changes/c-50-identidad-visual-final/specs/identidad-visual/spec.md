## Purpose

Fija la identidad visual final del producto (nombre, logo, tipografía y paleta) en un solo lugar del design system, para que todas las pantallas la tomen de ahí y cumplan el contraste mínimo.

## ADDED Requirements

### Requirement: Nombre del producto

La SPA SHALL mostrar el nombre "OdontologIA" tomado de `BRAND`, en el título de la pestaña del navegador y en la marca del layout y de la pantalla de ingreso. El nombre MUST NOT incluir el texto "provisional".

#### Scenario: La pestaña y la marca muestran el nombre final
- **GIVEN** la SPA cargada
- **WHEN** se lee el nombre de `BRAND`
- **THEN** es "OdontologIA", no contiene "provisional" y es el título del documento

### Requirement: Logo final accesible

El logo SHALL ser un SVG local con un título accesible que nombre el producto. MUST NOT contener el rótulo "LOGO PROVISIONAL" ni referencias a recursos remotos.

#### Scenario: El logo no es el placeholder
- **WHEN** se lee `design-system/assets/logo.svg`
- **THEN** tiene un `<title>` con "OdontologIA", no contiene "PROVISIONAL" y no carga recursos de otra URL

### Requirement: Tipografía autoalojada

La tipografía principal SHALL ser Public Sans, servida desde los archivos del propio build. La SPA MUST NOT pedir fuentes a un servidor externo, y el stack MUST terminar en fuentes de sistema por si la fuente no carga.

#### Scenario: La fuente se declara sin servidores externos
- **WHEN** se lee `--font-sans` en `design-system/tokens.css` y las hojas de estilo de la SPA
- **THEN** `--font-sans` empieza por "Public Sans Variable" y termina en `sans-serif`, y ninguna hoja importa fuentes de una URL remota

### Requirement: Paleta con contraste mínimo

El color primario SHALL ser verde bosque `#1B3A2F` y el fondo crema `#F5F1E8`. Cada par de texto y fondo de la paleta, en modo claro y oscuro, MUST tener un contraste de al menos 4.5:1, incluido el texto del menú lateral.

#### Scenario: Los pares de texto principales superan 4.5:1
- **GIVEN** los valores de `design-system/tokens.css` en modo claro y oscuro
- **WHEN** se calcula el contraste de texto sobre primario, texto sobre fondo y superficie, texto atenuado, enlaces y texto del menú lateral
- **THEN** cada par es de al menos 4.5:1

#### Scenario: Los dos bloques del modo oscuro son iguales
- **WHEN** se comparan las variables del bloque `prefers-color-scheme: dark` con las del bloque `[data-theme="dark"]`
- **THEN** declaran las mismas variables con los mismos valores
