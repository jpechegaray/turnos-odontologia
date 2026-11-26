# Tasks

Aplicar TDD estricto por tarea: primero el test de Vitest en rojo, después el código. Usar solo datos ficticios y el nombre exacto del escenario en `it(...)` cuando el test cubra la spec. Cada pantalla se muestra corriendo en la app real a 390x844 y 1440x900, y se pregunta antes de su commit.

## 1. Base: identidad y layout

- [x] 1.1 Instalar `@fontsource-variable/public-sans` e importarla en `main.tsx`; verificar "La fuente se declara sin servidores externos".
- [x] 1.2 Pasar `design-system/tokens.css` a la Revisión 2 (paleta bosque y crema, tokens de navegación, profesionales, agenda, radios y tipografía) con los dos bloques oscuros iguales; verificar "Los pares de texto principales superan 4.5:1" y "Los dos bloques del modo oscuro son iguales".
- [x] 1.3 Cambiar `BRAND_NAME` a "OdontologIA" y redibujar `assets/logo.svg`; verificar "La pestaña y la marca muestran el nombre final" y "El logo no es el placeholder".
- [x] 1.4 Sumar `grupo` e `icono` a `EntradaMenu`, los íconos de navegación al kit UI y el grupo de cada `routes.ts`; verificar "El menú agrupa las entradas por sección" y "Un grupo sin entradas para el rol no se muestra".
- [x] 1.5 Rehacer `Layout.tsx` y su CSS con el menú lateral verde y la barra inferior; verificar "El layout muestra la marca, la navegación y la pantalla actual" y "El botón "Más" abre y cierra el menú completo".
- [x] 1.6 Ajustar el kit UI (`shared/ui/ui.css`) y `index.css` a la Revisión 2.

## 2. Pantallas del personal

- [x] 2.1 Ingresar: panel de marca y tarjeta de ingreso según el mockup.
- [x] 2.2 Inicio: recuadros de las secciones y resumen del día; reemplazar la barra inferior del celular por el botón "Menú"; verificar "Inicio muestra un recuadro por sección permitida", "El botón "Menú" abre y cierra el menú completo", "Inicio muestra las cifras del día", "Inicio lista los próximos turnos", "Inicio sin turnos pendientes" y "Un fallo al cargar el resumen permite reintentar".
- [ ] 2.3 Agenda, con crear, reprogramar, cancelar y asistencia.
- [ ] 2.4 Pacientes.
- [ ] 2.5 Lista de espera.
- [ ] 2.6 Profesionales, con horarios, bloqueos y prestaciones.
- [ ] 2.7 Sillones y prestaciones.
- [ ] 2.8 Usuarios, configuración y notificaciones.

## 3. Pantallas públicas

- [ ] 3.1 Reserva pública, turno por token y oferta de hueco.

## 4. Cierre

- [ ] 4.1 Actualizar `design-system/README.md` (Revisión 2, `Approval: pending`, sin `(TBD)`, nota de emails de texto plano), `brand.md` y `components.html`, y regenerar las capturas de `previews/`.
- [ ] 4.2 Correr `npm test`, `npm run lint` y `npm run build`, y escribir `verify.md` con la revisión visual de cada pantalla.
- [ ] 4.3 Actualizar la ficha de C-50 en `CHANGES.md` y abrir el PR.
