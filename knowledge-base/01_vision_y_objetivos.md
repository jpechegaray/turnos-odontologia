# Visión y Objetivos

## Propósito del sistema

**Turnos Odontología** es una aplicación web (API REST + SPA) para que un consultorio odontológico argentino de 2 a 5 profesionales gestione su agenda sin turnos superpuestos, ni por profesional ni por sillón (box), y recupere los huecos que dejan las cancelaciones mediante una lista de espera.

El producto se enfoca en la **agenda y los turnos**, no en la gestión clínica integral. Se instala **un consultorio por instalación** (single-tenant). Los pacientes reservan y gestionan su turno online **sin crear cuenta**, mediante un enlace con token. Es un proyecto académico (UTN, equipo de 6 personas) que usa solo datos ficticios.

## Posicionamiento y mercado

Fuente: `docs/discovery/informe-discovery.md` (23 sistemas relevados, consulta del 2026-11-20). Casi toda la evidencia es **afirmación comercial**; lo comprobado se limita a precios, documentación técnica y algunos centros de ayuda. Ningún producto fue probado por el equipo.

### Competidores de referencia

| Grupo | Sistemas | Qué aportan como referencia |
|---|---|---|
| Argentina (7) | DentalCore, Dentia, DentalSoft, DentalTec, Medicloud, Calu, Docturno | Cubren lo fiscal y lo local (ARCA, Mercado Pago, obras sociales declarados). Ninguno evidencia sillones, duración variable y solapamientos a la vez. |
| Latinoamérica (8) | Dentalink, Simples Dental, Clinicorp, Doctocliq, Medilink, Encuadrado, AgendaPro, Dentidesk | Simples Dental declara control de sillones sin horarios duplicados. Dentalink tiene un módulo de ayuda para Argentina (solo se leyeron los títulos). |
| Internacionales (8) | DriCloud/XDentalCloud, Open Dental, Nubimed, Dentally, Curve Dental, Dentrix Ascend, Clinic Cloud, Gesden | Open Dental es el único con reserva online y lista ASAP documentadas públicamente. Dentally publica precio por sillón. |

Ranking preliminar del informe (no concluyente, diferencias de décimas): DriCloud 3,70; DentalCore 3,58; Dentia 3,50; Open Dental 3,38; Nubimed 3,28.

**Referencias de UX:** Open Dental (Web Sched y lista ASAP con enlace único por paciente), Dentally (portal del paciente) y AgendaPro (reserva estilo e-commerce).

### Vacíos del mercado argentino que ataca el producto

1. **El detalle de agenda casi no se publica:** solo 2 de 23 declaran prevención de solapamientos, 7 gestión por sillón y 3 duración variable.
2. **Ningún sistema argentino evidencia a la vez** agenda por sillón, duración por prestación y prevención de solapamientos.
3. **Reserva online del paciente no evidenciada** en DentalCore, DentalTec ni Dentia.
4. **Política de cancelación:** ninguna ficha documenta plazo configurable ni registro de cancelaciones tardías.
5. **Lista de espera sin criterio de compatibilidad:** existe en 6 de 23, pero ninguna ficha describe que proponga solo pacientes compatibles (profesional, prestación, duración).
6. **Complejidad para recepción:** hay productos con hasta 17 perfiles de rol (Dentia); un rol de recepción mínimo es un posible diferencial de usabilidad.

### Diferenciador

**Lista de espera que avisa sola, por email, a los pacientes compatibles cuando se libera un hueco; el primero que confirma se queda con el turno.** Se apoya en la agenda por sillón y en la duración por prestación, que el MVP ya incluye. DriCloud avisa a todos y gana el primero que reserva; Open Dental envía un enlace ASAP. Ninguna ficha describe un filtro por compatibilidad.

## Objetivos por actor

| Actor | Objetivo principal | Objetivos secundarios |
|---|---|---|
| Odontólogo | **Atender** su agenda: ver el día y la semana por profesional y sillón (solo lectura) | Consultar la ficha básica de sus pacientes; pedir cambios a recepción fuera del sistema |
| Recepción | **Agendar** turnos de todos los profesionales con el menor esfuerzo posible | Reprogramar y cancelar; marcar asistencia; gestionar horarios y bloqueos; administrar fichas de pacientes; supervisar la lista de espera |
| Administrador | **Configurar** el consultorio | Prestaciones y duraciones, sillones, profesionales, usuarios, plazo de cancelación |
| Paciente (anónimo con token) | **Reservar** online sin cuenta | Ver, confirmar o cancelar su turno; recibir un recordatorio por email |

## Alcance v1.0 (MVP)

Exactamente el de `docs/discovery/discovery.md` §5:

- Agenda por profesional y por sillón/box con **prevención de solapamientos** (RN-01, RN-02).
- Duración del turno según la prestación, ajustable al darlo (RN-03).
- Horarios de atención y bloqueos por profesional (RN-04).
- Crear, reprogramar y cancelar turnos, con plazo de cancelación configurable (RN-05, RN-06).
- Reserva online del paciente sin login, con token (RN-07, RN-08).
- Ficha básica de paciente: datos de contacto, obra social como dato y notas.
- Recordatorio por email con SMTP real.
- Roles ODONTOLOGO, RECEPCION y ADMIN.
- **Diferenciador:** lista de espera que avisa automáticamente a los pacientes compatibles al liberarse un hueco (gana el primero que confirma).

## Fuera de alcance (etapas posteriores)

Fuente: `discovery.md` §6. Se incluyen como épicas futuras en `06_funcionalidades.md`.

- Odontograma, periodontograma e historia clínica completa.
- WhatsApp (recordatorios y confirmaciones): requiere cuenta de Meta y tiene costo por mensaje.
- Mercado Pago y señas.
- Facturación electrónica ARCA.
- Validación online de obras sociales y prepagas (en el MVP la obra social es solo un dato).
- Multisede, multi-tenant, liquidaciones y reportes avanzados.
- Campañas y recuperación de pacientes.
- Asistentes con IA.
- Login o cuenta de pacientes.
- Integración con Google Calendar.

## Métricas de éxito

Para una entrega académica, el éxito se mide con criterios verificables:

- **Cero solapamientos** posibles por profesional y por sillón, incluso con concurrencia: lo garantiza la restricción en base de datos, verificada con tests de integración (Testcontainers).
- Todos los escenarios de borde de RN-01 a RN-08 cubiertos por tests automatizados, incluido "un turno que empieza justo cuando termina otro no se solapa".
- Un paciente completa una reserva online en menos de 2 minutos sin crear cuenta (**Suposición:** objetivo de usabilidad, a validar en demo).
- Recepción da un turno en 4 pasos o menos (**Suposición:** objetivo de usabilidad).
- Los recordatorios se envían una sola vez por turno y los fallos de SMTP quedan registrados.
- Cero secretos y cero datos reales en el repositorio.
