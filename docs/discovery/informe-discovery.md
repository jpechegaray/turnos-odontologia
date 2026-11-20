# Informe de Discovery
## Sistemas de gestión de turnos y agenda para consultorios odontológicos del mercado argentino

**Materia:** Metodología I — Tecnicatura Universitaria en Programación (UTN)

**Integrantes:** Juan Pablo (líder), Santino, Valentino, Lionel, Julián, Nico

**Fecha:** 2026-11-20

---

## Resumen ejecutivo

**Qué se investigó.** Se relevaron 23 sistemas de gestión de turnos y agenda, odontológicos o de salud en general, para identificar qué ofrece hoy el mercado, qué falta en Argentina y qué debería incluir el primer producto del equipo. La fuente fue exclusivamente pública y web (sitios oficiales, páginas de precios, centros de ayuda, documentación y algunos marketplaces). Cada dato se clasificó como **Comprobado** (lo muestra documentación, centro de ayuda, tabla de planes o tarifas), **Afirmación comercial** (lo declara la landing sin demostrarlo) o **No evidenciado**. Todas las consultas se hicieron el 2026-11-20.

**Hallazgos principales.**

1. **Casi toda la evidencia es comercial.** Hay pocas funciones comprobadas: la reserva online, la lista ASAP y la API REST de Open Dental, la documentación funcional de obras sociales de DentalSoft y los precios publicados. Ningún sistema evidencia, con material comprobado, ARCA, validación de obras sociales o WhatsApp.
2. **El detalle de agenda casi nunca se publica.** Solo 2 de 23 sistemas declaran prevención de solapamientos (DentalSoft y Simples Dental), 7 declaran o dejan ver gestión por sillón o box, y 3 muestran duración variable por prestación. Ninguna ficha documenta una política de cancelación con plazo configurable.
3. **Los productos argentinos cubren lo fiscal y lo local, pero no lo evidencian en agenda.** DentalCore, Dentia, DentalSoft, DentalTec y Medicloud declaran ARCA, Mercado Pago y/u obras sociales, pero ninguno evidencia sillones, duración variable ni solapamientos juntos. Además, DentalCore y DentalTec no evidencian reserva online del paciente.
4. **La lista de espera existe en pocos productos (6 de 23) y ninguna ficha describe que proponga pacientes compatibles.** DriCloud avisa a todos y gana el primero en reservar; Open Dental ofrece una lista ASAP con enlace único por paciente.
5. **Ranking preliminar (total ponderado recalculado):** DriCloud/XDentalCloud 3,70; DentalCore 3,58; Dentia 3,50; Open Dental 3,38; Nubimed 3,28. Las diferencias de décimas entre productos no son significativas, porque la base es comercial.

**MVP recomendado (decisión del equipo, respaldada por la evidencia).** Agenda por profesional y por sillón/box sin solapamientos ni sobreturnos; duración por prestación; bloqueos y horarios por profesional; crear, reprogramar y cancelar turnos con cancelación sin penalidad hasta 24 hs (configurable); reserva online del paciente sin login mediante enlace/token único; ficha básica de paciente con obra social como dato; recordatorio por email real (SMTP); y tres roles (odontólogo, recepción, administrador). Como **diferenciador**, una lista de espera que propone pacientes compatibles cuando se libera un hueco. Quedan para **etapas posteriores** odontograma/historia clínica, WhatsApp, Mercado Pago y señas, ARCA, validación de obras sociales, multisede, campañas e IA.

**Demos prioritarias:** DriCloud/XDentalCloud, DentalCore, Dentia, DentalSoft y Open Dental. **Referencias de UX:** Open Dental (Web Sched/ASAP), Dentally (portal del paciente) y AgendaPro (reserva estilo e-commerce).

> **Criterios de clasificación usados en todo el informe.** *Comprobado* significa que el dato figura en documentación, centro de ayuda, tabla de planes o tarifas publicada, o en una página de precios. Eso no equivale a haber probado el producto. *Afirmación comercial* es lo declarado en landings o por terceros sin demostración. *No evidenciado* es la ausencia de respaldo público y puntúa como ausente. Un precio publicado se marca como Comprobado en el sentido de "figura publicado". El equipo no verificó ni probó ningún producto (ver sección Verificación de fuentes).

---

## A. Tabla comparativa de sistemas

Se presentan 23 sistemas, ordenados por relevancia para Argentina (Alta, Media, Baja) y, dentro de cada grupo, por total ponderado de la matriz B. En los empates se usa orden alfabético. La tabla se divide en A.1 (identificación y modalidad) y A.2 (funcionalidades) con las mismas filas. Cada celda dice "Comprobado", "Afirmación comercial" o "No evidenciado". Doctocliq figura como "Media-Baja" en su ficha y se ubica al final del grupo Media. La **fecha de consulta es 2026-11-20 para todas las fuentes**.

### A.1 Identificación, segmento, modalidad y precio

| # | Producto / Proveedor | Relevancia AR | País | Segmento | Modalidad | Precio publicado | Fuente principal (consulta 2026-11-20) |
|---|---|---|---|---|---|---|---|
| 1 | **DriCloud / XDentalCloud** — DriCloud (razón social: No evidenciado) | Alta | Afirmación comercial (España: precios en EUR, VeriFactu) | Afirmación comercial (profesional independiente hasta grandes clínicas y franquicias; odontología vía XDentalCloud) | Afirmación comercial (SaaS en nube, Azure; apps iOS y Android) | Comprobado (EUR/mes, IVA incluido: Esencial 42 €, Solo 92 €, Clínica 129 €, Centro médico 249 €, Grandes clínicas 497 €). Precios en USD para América: No evidenciado | https://www.dricloud.com/ ; https://xdentalcloud.com/ |
| 2 | **DentalCore** — Alfredo Di Tullio, La Plata | Alta | Afirmación comercial (Argentina, La Plata) | Afirmación comercial (consultorio y clínica con varios profesionales, varias clínicas por cuenta). Cadena/laboratorio: No evidenciado | Afirmación comercial (SaaS en navegador y app de escritorio Windows) | Comprobado (publicado en ARS: $75.000/mes con 2 usuarios, +$15.000 por usuario adicional). La FAQ del mismo sitio dice USD 50/mes: discrepancia | https://dentalcore.app/ |
| 3 | **Dentia** — dentia.tech | Alta | Afirmación comercial (Argentina) | Afirmación comercial (consultorio, clínica y cadena con sucursales) | Afirmación comercial (web app instalable en PC, tablet y celular) | Comprobado (ARS/mes: Básico $20.000, Pro $30.000, Elite $40.000; anual $200.000/$300.000/$400.000). Prueba de 30 días: Afirmación comercial | https://dentia.tech/ |
| 4 | **DentalSoft** — dentalsoft.com.ar | Alta | Afirmación comercial (Argentina, La Plata y Rosario) | Afirmación comercial (consultorio/clínica, centro médico/infantil, multisucursal). Cadena/laboratorio: No evidenciado | Afirmación comercial (SaaS 100 % nube) | Comprobado (con "$" sin código de moneda: Gratis $0 por 3 meses, Gestión $30.000/mes, Pro $60.000/mes; Meta cobra USD 0,026 por mensaje de WhatsApp automático) | https://dentalsoft.com.ar/ |
| 5 | **Medicloud** — plataforma argentina de gestión médica (no odontológica) | Alta (local, sin odontología) | Afirmación comercial (Argentina) | Afirmación comercial (profesionales y equipos de salud en general). Odontología: No evidenciado | Afirmación comercial (SaaS web y apps iOS/Android) | Comprobado (ARS, IVA incluido: Gratis $0, PRO $18.400/mes, Team $29.440/mes para 2 médicos; WhatsApp desde $9680 por 100 mensajes; bot WhatsApp $150.000/mes) | https://www.medicloud.com.ar/precios |
| 6 | **Dentalink** — Software Dentalink / Atomy | Alta | Afirmación comercial de tercero (Chile, Santiago; según Capterra vía búsqueda) | Afirmación comercial (consultorio/clínica, grupos, multicentro, laboratorio como módulo) | Afirmación comercial (SaaS 100 % nube). App móvil: No evidenciado | No evidenciado (planes Esencial/Pro/Titanium sin montos). "Desde 29,00 US$" en Capterra: Afirmación comercial de tercero, solo en snippet de búsqueda | https://web-es.softwaredentalink.com/planes |
| 7 | **DentalTec** — Tándem Digital (San Juan) | Alta | Afirmación comercial (Argentina, San Juan) | Afirmación comercial (odontólogo/consultorio/clínica, círculos y federaciones odontológicas, módulo de obras sociales) | Afirmación comercial (100 % web) | No evidenciado para el software. Mensajería WhatsApp: Comprobado como publicado (USD 9 por 50 mensajes, USD 25 por 150, USD 48 por 300) | https://web.dentaltec.com.ar/ |
| 8 | **Open Dental** — Open Dental Software, Inc. | Media | Comprobado (Estados Unidos; tarifas por país) | Afirmación comercial (consultorios de cualquier tamaño). Comprobado (multiubicación con tarifa por volumen) | Afirmación comercial (escritorio con base de datos y eServices en la nube). Ya no es open source desde v24.4 (cambio de licencia vía búsqueda) | Comprobado (USD/mes por ubicación: EE.UU. $199 y luego $149; Canadá $164 y luego $137; "Other Countries" $89; "Developing Countries" gratis con soporte limitado; eServices por módulo, p. ej. Web Sched $75) | https://www.opendental.com/site/fees.html |
| 9 | **Doctocliq** — Doctocliq (Perú) | Media-Baja | Afirmación comercial (Perú, según prensa vía búsqueda) | Afirmación comercial (clínica dental, médicos, estética, independientes) | Afirmación comercial (SaaS "100 % online"; app en App Store) | Comprobado (USD: Gratis 0, Individual 19, Básico 29, Avanzado 49; la página no aclara si el monto es mensual o equivalente anual) | https://www.doctocliq.com/planes-y-precios |
| 10 | **Calu** — calu.app | Media | No evidenciado (se sospecha Argentina por Mercado Pago, sin confirmar) | Afirmación comercial (profesional independiente y centro de salud). Odontología: No evidenciado | Afirmación comercial (web SaaS). App móvil: No evidenciado | No evidenciado el monto mensual. Costo por día por agenda "menos de" $1.200 a $2.200, sin moneda: Afirmación comercial. Prueba de 7 días: Afirmación comercial | https://www.calu.app/planes |
| 11 | **Docturno** — Grupo Cormos | Media | Afirmación comercial (Argentina; indicio por ayuda en cormos.com.ar y nota de prensa) | Afirmación comercial (profesionales independientes, grupos médicos y clínicas). Odontología: No evidenciado | Afirmación comercial (web, SaaS con marketplace para pacientes). App móvil: No evidenciado | No evidenciado (remite a planes para profesionales) | https://profesionales.docturno.com/ |
| 12 | **Nubimed** — razón social No evidenciado | Baja | Afirmación comercial (España) | Afirmación comercial (clínicas dentales con énfasis y clínicas médicas multiespecialidad) | Afirmación comercial (SaaS en navegador) | Comprobado (EUR/mes sin IVA: Lite 27,40 €, Premium 50,10 €, Multicentro "Consultar"; SMS 0,08 €) | https://www.nubimed.com/planes-precios/ |
| 13 | **Dentally** — Henry Schein One UK | Baja | Comprobado (Reino Unido; precios en GBP) | Afirmación comercial (grupos multisede y prácticas NHS). Comprobado (plan desde 1 "surgery") | Afirmación comercial (nube). Comprobado en tabla de planes (usuarios y almacenamiento ilimitados) | Comprobado (GBP/mes por sede, sin IVA, por cantidad de sillones: Starter £134 a £618, Essentials £238 a £778, Pro £346 a £1,021 para 1 a 8 surgeries) | https://www.dentally.com/en-gb/pricing |
| 14 | **Curve Dental** — razón social No evidenciado | Baja | Afirmación comercial (EE.UU./Canadá) | Afirmación comercial (consultorios independientes, multiubicación/DSO, startups) | Afirmación comercial (nube nativa en navegador; app Curve Mobile) | No evidenciado (cotización a pedido; Comprobado que la página /pricing no muestra cifras) | https://www.curvedental.com/pricing |
| 15 | **Clinicorp** — Clinicorp | Baja | Afirmación comercial (Brasil) | Afirmación comercial (clínicas y consultorios odontológicos, estética, franquicias) | Afirmación comercial (nube, 10 GB incluidos) | Comprobado (BRL/mes: Standard R$ 159,90, Premium R$ 369,90, Combo IA R$ 468,90; la landing muestra Standard R$ 149,90: discrepancia entre páginas; implementación obligatoria R$ 149,90 o R$ 299) | https://www.clinicorp.com/planos |
| 16 | **Dentrix Ascend** — Henry Schein One | Baja | Afirmación comercial (Estados Unidos) | Afirmación comercial (consultorios y multiubicación/DSO) | Afirmación comercial (100 % nube) | No evidenciado (lista de precios). "Ascend Starter US$199/mes" citado en la home: Afirmación comercial, moneda inferida por el signo $ | https://www.dentrixascend.com/ |
| 17 | **Simples Dental** — Simples Dental | Baja | Afirmación comercial (Brasil) | Afirmación comercial (dentistas, clínicas, secretarias y administradores) | Afirmación comercial (nube; app para dentistas y pacientes) | Comprobado (BRL, página Black Friday: Basic R$ 149,90 (promo 112,42), Plus R$ 249,90 (187,42), Pro R$ 349,90 (320,74); la página muestra además otros montos sin aclarar modalidad; extras sin precio) | https://www.simplesdental.com/planos-e-precos |
| 18 | **Clinic Cloud** — Doctoralia (Clinic Cloud) | Baja | Afirmación comercial (España) | Afirmación comercial (clínicas y profesionales sanitarios en general). Comprobado (odontograma solo en plan Max) | No evidenciado (nube/local/híbrido). Portal del paciente: Afirmación comercial | Comprobado (EUR/mes sin IVA: Mini 29 € (promo 14,5), Pro 49 € (24,5), Max 79 € (39,5), Enterprise a cotizar; promo = 50 % los primeros 3 meses) | https://www.clinic-cloud.com/tarifas |
| 19 | **Medilink** — Software Medilink Latam | Baja | No evidenciado (contacto +56 Chile; también Colombia y México) | Afirmación comercial (centros médicos y consultas de salud). Odontología: No evidenciado | Afirmación comercial (software en línea). App móvil: No evidenciado | No evidenciado (planes "Solicita una Demo"). Único precio visible: Contact Center IA USD 50/mes, Afirmación comercial | https://www.softwaremedilink.com/planes |
| 20 | **Encuadrado** — Encuadrado | Baja | Afirmación comercial (Chile; versión /mx) | Afirmación comercial (salud no odontológica: psicología, nutrición, kinesiología, estética). Odontología: No evidenciado | Afirmación comercial (web y móvil) | Comprobado (publicado con "$" sin moneda explícita, probablemente CLP sin confirmar: Esencial $26.107 (0,6 UF), Profesional $43.511 (1 UF), Avanzado $141.218 (3 UF); primer mes promo $10.000) | https://www.encuadrado.com |
| 21 | **AgendaPro** — AgendaPro | Baja | No evidenciado (ruta /co/; "número 1 en Colombia" es Afirmación comercial) | Afirmación comercial (belleza, spa, estética, fisioterapia, clínicas). Odontología: No evidenciado | Afirmación comercial (SaaS con apps iOS y Android) | No evidenciado (la página /co/planes devolvió 404) | https://www.agendapro.com |
| 22 | **Dentidesk** — Dentidesk | Baja | Afirmación comercial (Chile, Santiago) | Afirmación comercial (clínica dental, centros médicos, hospitales, instituciones educacionales) | Afirmación comercial (plataforma web con login). Nube/app móvil: No evidenciado | No evidenciado (demo gratis y prueba de 15 días: Afirmación comercial) | https://www.dentidesk.cl |
| 23 | **Gesden (G5 / Gesden ONE)** — Infomed (Henry Schein) | Baja | Afirmación comercial de terceros (España) | Afirmación comercial de terceros (clínicas dentales) | Afirmación comercial de terceros (G5 escritorio Windows; ONE web en la nube) | Afirmación comercial de terceros (ONE desde 75 €/mes sin IVA, 104 € con Pack Premium; otra tabla del mismo competidor indica 49 €/mes: inconsistentes, no comprobado) | https://pabau.com/es/blog/alternativas-a-gesden/ (sitio oficial no accesible) |

### A.2 Funcionalidades: agenda, reserva, recordatorios, gestión clínica, facturación y obras sociales

| # | Producto | Agenda (sillones/boxes y solapamientos) | Reserva online | Recordatorios / WhatsApp | Gestión clínica (odontograma) | Facturación ARCA / obras sociales |
|---|---|---|---|---|---|---|
| 1 | **DriCloud / XDentalCloud** | Afirmación comercial (varios sillones, doctores y sedes; duración predefinida por tratamiento). Bloqueos, sobreturnos y solapamientos: No evidenciado | Afirmación comercial (sitio web y enlace; chatbot IA; lista de espera con aviso a todos y gana el primero que reserva). Cambios por el paciente: No evidenciado | Afirmación comercial (email gratis, SMS 0,09 €, push, WhatsApp API oficial de Meta a 40 $/mes) | Afirmación comercial (odontograma, periodontograma, planes, presupuestos, imágenes) | Afirmación comercial (ARCA A/B/C con CAE, Mercado Pago). Obras sociales argentinas: No evidenciado |
| 2 | **DentalCore** | Afirmación comercial (vistas día a mes, multi-doctor, bloqueos). Sillones, duración, sobreturnos y solapamientos: No evidenciado | No evidenciado (portal del paciente sin verificar). Confirmar/cancelar con enlace: Afirmación comercial. Lista de espera: No evidenciado | Afirmación comercial (WhatsApp API de Meta a 48 y 24 h, email, SMS) | Afirmación comercial (odontograma, periodontograma, presupuestos, imágenes) | Afirmación comercial (ARCA A/B/C/E con CAE; 65 obras sociales cargadas, cálculo de cobertura y liquidación; no valida en línea contra la obra social, lo aclara el propio sitio) |
| 3 | **Dentia** | Afirmación comercial (multiprofesional, sobreturnos, Google Calendar). Vistas, sillones, duración, bloqueos y solapamientos: No evidenciado | No evidenciado (reserva propia del paciente). Afirmación comercial (asistente IA por WhatsApp que agenda, reprograma y cancela; seña en plan Elite). Lista de espera: No evidenciado | Afirmación comercial (WhatsApp y email registrados en agenda; reseñas, cumpleaños) | Afirmación comercial (odontograma, periodontograma, presupuestos, consentimientos, IA de radiografías en Elite) | Afirmación comercial (ARCA A/B/C, Mercado Pago, liquidación por obra social y prepaga). Nomencladores: No evidenciado |
| 4 | **DentalSoft** | Afirmación comercial (día/semana/mes, multiprofesional, bloqueos por feriados, sobreturnos, detección automática de solapamientos, Google Calendar). Sillones y duración variable: No evidenciado | Afirmación comercial (plan Pro: 4 pasos, filtro por especialidad y obra social; portal; recepcionista IA por WhatsApp). Lista de espera: No evidenciado | Afirmación comercial (WhatsApp manual con 1 clic; automático en Pro) | Afirmación comercial (odontograma FDI de 18 estados, ortodoncia, presupuestos). Periodontograma: No evidenciado | Comprobado (documentación funcional de obras sociales: cobertura por plan, coseguro, liquidación por período). Afirmación comercial (Mercado Pago en Pro). ARCA: No evidenciado |
| 5 | **Medicloud** | Afirmación comercial (equipo, sobreturnos, multiconsulta, turnos recurrentes). Sillones, bloqueos y solapamientos: No evidenciado | Afirmación comercial (24 h desde la web del profesional; pago obligatorio en PRO; bot de WhatsApp). Lista de espera: No evidenciado | Afirmación comercial (email; WhatsApp en paquete aparte) | No evidenciado (odontograma). Afirmación comercial (historia clínica general) | Afirmación comercial (ARCA A/B/C en PRO/TEAM, Mercado Pago, PayPal). Liquidación de obras sociales: No evidenciado |
| 6 | **Dentalink** | Detalle de agenda: No evidenciado (sillones, bloqueos, solapamientos). Múltiples profesionales: Afirmación comercial | Afirmación comercial (24/7, redes sociales, contact center IA). Cancelación y lista de espera: No evidenciado | Afirmación comercial (WhatsApp y correo; reagendamiento de no asistidos) | Afirmación comercial (odontograma, periodontograma, ortodoncia, firma digital) | Obras sociales de Argentina: Comprobado solo como existencia de artículos de ayuda (contenido no leído). ARCA: No evidenciado |
| 7 | **DentalTec** | Afirmación comercial (multiprofesional, intervalos configurables). Vistas, sillones, bloqueos, sobreturnos y solapamientos: No evidenciado | No evidenciado (la FAQ no la menciona) | Afirmación comercial (WhatsApp oficial; confirmación respondiendo el mensaje) | Afirmación comercial (odontograma, CIE-10, adjuntos). Periodontograma y presupuestos: No evidenciado | Afirmación comercial (ARCA; validación en tiempo real con más de 15 obras sociales por servicios SOAP; liquidaciones) |
| 8 | **Open Dental** | Comprobado indirectamente (operatorios en la API REST). Afirmación comercial (bloqueos, multiprofesional). Duración, sobreturnos y solapamientos: No evidenciado | Comprobado (Web Sched para paciente nuevo, existente y recall; lista ASAP con enlace único por paciente para aceptar o rechazar el hueco) | Comprobado (eReminders por email y texting integrado en tarifas). WhatsApp: No evidenciado | Afirmación comercial (odontograma 3D, recetas). Comprobado en tarifa (IA de imágenes). Periodontograma y presupuestos: No evidenciado | No evidenciado (ARCA y obras sociales). Comprobado (portal de pagos) |
| 9 | **Doctocliq** | Afirmación comercial (multiprofesional, agrupación por equipo). Sillones, bloqueos y solapamientos: No evidenciado | Afirmación comercial (agendamiento en línea desde plan Individual; asistente Soyla IA). Cancelación y lista de espera: No evidenciado | Afirmación comercial (WhatsApp con costo según país) | Afirmación comercial (odontograma, presupuestos, ortodoncia). Periodontograma y radiografías: No evidenciado | No evidenciado (ARCA y obras sociales). Afirmación comercial (facturación de Perú, México, Colombia y Ecuador) |
| 10 | **Calu** | Afirmación comercial (multiagenda y sedes). Sillones, duración, bloqueos y solapamientos: No evidenciado | Afirmación comercial (autogestión 24 h desde plan Automatiza; cobro anticipado con Mercado Pago) | Afirmación comercial (WhatsApp con 50, 200, 1000 o ilimitados según plan) | No evidenciado (odontograma). Afirmación comercial (historia clínica general) | Afirmación comercial (Mercado Pago). ARCA y obras sociales: No evidenciado |
| 11 | **Docturno** | Comprobado solo a nivel de categoría de ayuda (tipos de turno y días no laborables; detalle no leído). Afirmación comercial (grupos médicos). Sillones y solapamientos: No evidenciado | Afirmación comercial (24/7 con búsqueda por cobertura, página propia, bot de WhatsApp) | Afirmación comercial (email, SMS y WhatsApp a 24 h y 3 h) | No evidenciado | Afirmación comercial (Mercado Pago). ARCA y obras sociales (más allá del filtro por cobertura): No evidenciado |
| 12 | **Nubimed** | Afirmación comercial (profesionales y horarios; sala de espera). Sillones, duración, bloqueos y solapamientos: No evidenciado | Afirmación comercial (24 h, portal, confirmar/cancelar desde el recordatorio, lista de espera) | Comprobado como tarifa (email gratis, SMS 0,08 €). Afirmación comercial (WhatsApp con IA) | Afirmación comercial (odontograma, periodontograma, presupuestos desde odontograma). Imágenes: No evidenciado | Comprobado como inclusión de plan (TicketBAI/Verifactu de España). ARCA y obras sociales argentinas: No evidenciado |
| 13 | **Dentally** | Comprobado indirectamente (precio por 1 a 8 "surgeries"). Afirmación comercial (calendario personalizable). Duración, bloqueos y solapamientos: No evidenciado | Afirmación comercial (Portal con formularios, tablet y kiosk de check-in; lista de espera). Cancelación por el paciente: No evidenciado | Afirmación comercial (SMS y email). WhatsApp: No evidenciado | Afirmación comercial (carta dental, periodontograma con comparación en el tiempo, notas IA) | Comprobado como inclusión de plan (integración NHS). ARCA y obras sociales: No evidenciado |
| 14 | **Curve Dental** | Afirmación comercial (vistas por proveedor y por operatorio, bloqueos). Duración, sobreturnos y solapamientos: No evidenciado | Afirmación comercial (reserva online con permisos, autoagendado, lista "Smart Fill" de corto aviso). Cancelación por el paciente: No evidenciado | Afirmación comercial (texto y email bidireccionales, recare). WhatsApp: No evidenciado | Afirmación comercial (charting, periodontograma, imágenes, plan de tratamiento) | Afirmación comercial (seguros de EE.UU.). ARCA y obras sociales: No evidenciado |
| 15 | **Clinicorp** | Afirmación comercial (agenda inteligente, check-in con QR). Sillones, duración, bloqueos, sobreturnos y solapamientos: No evidenciado | Afirmación comercial (agendamiento online en plan Standard; asistente Cloudia/agentes IA). Cancelación y lista de espera: No evidenciado | Afirmación comercial (WhatsApp, alerta de retorno) | Afirmación comercial (prontuario, odontograma, fichas por especialidad). Periodontograma: No evidenciado | Afirmación comercial (Pix, boleto y NFS-e de Brasil). ARCA y obras sociales: No evidenciado |
| 16 | **Dentrix Ascend** | Afirmación comercial (disponibilidad por profesional). Vistas, sillones, bloqueos, sobreturnos y solapamientos: No evidenciado | Afirmación comercial (24/7, Reserve with Google, enlace por email). Lista de espera: No evidenciado | Afirmación comercial (recordatorios sin detalle de canal). WhatsApp: No evidenciado | Afirmación comercial (charting con IA, imágenes). Odontograma y periodontograma: No evidenciado | Afirmación comercial (seguros de EE.UU.). ARCA y obras sociales: No evidenciado |
| 17 | **Simples Dental** | Afirmación comercial (control de sillones sin horarios duplicados, bloqueos, encaje/sobreturno). Múltiples profesionales y duración: No evidenciado | Afirmación comercial (enlace 24/7 en tiempo real; secretaria IA por WhatsApp de pago aparte; lista de espera vía "encaje") | Afirmación comercial (WhatsApp, SMS y app del paciente; WhatsApp de pago aparte) | Afirmación comercial (prontuario, odontograma, ortodoncia). Periodontograma: No evidenciado | Afirmación comercial (Pix, boleto y nota fiscal de Brasil). ARCA y obras sociales: No evidenciado |
| 18 | **Clinic Cloud** | Afirmación comercial (multiprofesional, salas y boxes). Duración, bloqueos, sobreturnos y solapamientos: No evidenciado | Comprobado como inclusión de plan (vía Doctoralia; funcionamiento sin demostrar). Lista de espera: No evidenciado | Comprobado como inclusión de plan (recordatorios en todos los planes). WhatsApp: Afirmación comercial en la home, pero la página de agenda cita solo SMS y email | Comprobado como inclusión (odontograma solo en plan Max; funcionamiento sin demostrar). Periodontograma: No evidenciado | Comprobado como inclusión (TicketBAI/Verifactu). ARCA y obras sociales: No evidenciado |
| 19 | **Medilink** | Afirmación comercial (por profesional, control de bloques). Sillones, bloqueos puntuales y solapamientos: No evidenciado | Afirmación comercial (web, WhatsApp e Instagram, cobro al agendar; contact center IA) | Afirmación comercial (email y WhatsApp gestionado) | No evidenciado (odontograma). Afirmación comercial (ficha clínica genérica) | No evidenciado (ARCA y obras sociales) |
| 20 | **Encuadrado** | Afirmación comercial (vista semanal, bloqueos, reagendar, límite diario). Múltiples profesionales, sillones y solapamientos: No evidenciado | Afirmación comercial (reserva en el perfil; asistente de WhatsApp) | Afirmación comercial (WhatsApp y correo) | No evidenciado (odontograma). Afirmación comercial (ficha genérica) | Afirmación comercial (boleta electrónica SII de Chile). ARCA y obras sociales: No evidenciado |
| 21 | **AgendaPro** | Afirmación comercial (agenda online, multisucursal). Profesionales, sillones, bloqueos y solapamientos: No evidenciado | Afirmación comercial (sitio 24/7, asistente IA Sofia, Reserve with Google) | Afirmación comercial (WhatsApp y email) | No evidenciado | No evidenciado (ARCA y obras sociales). Afirmación comercial (pagos y cierres de caja) |
| 22 | **Dentidesk** | Afirmación comercial (usuarios con agenda). Sillones, duración, bloqueos y solapamientos: No evidenciado | Afirmación comercial (add-on de agendamiento online sin detalle; reprogramación) | Afirmación comercial (recordatorios). WhatsApp automático: anunciado como "próximamente", No evidenciado como funcional | Afirmación comercial (ficha clínica y odontograma con tres nomenclaturas) | Afirmación comercial (SII de Chile; Alegra/DGII en República Dominicana). ARCA y obras sociales: No evidenciado |
| 23 | **Gesden** | Afirmación comercial de terceros (agenda multigabinete). Resto: No evidenciado | Afirmación comercial de terceros ("puede depender de módulos adicionales") | No evidenciado (recordatorios). Afirmación comercial de terceros (comunicación con pacientes requiere módulos adicionales) | Afirmación comercial de terceros (odontograma, periodontograma, presupuesto) | Afirmación comercial de terceros (VeriFactu de España). ARCA y obras sociales: No evidenciado |

**Lectura crítica de las tablas.** Los casos en que la ficha marca "Comprobado" corresponden a información publicada en tablas de planes, tarifas o documentación (por ejemplo, la reserva y la lista ASAP de Open Dental, o la documentación de obras sociales de DentalSoft). Sirven para saber qué ofrece el proveedor, pero no sustituyen una prueba del producto. Las fichas de Clinic Cloud, Nubimed y Dentally usan "Comprobado como inclusión de plan", que significa que la función figura en el plan y no que se haya visto funcionar.

---

## B. Matriz de puntuación 0-5

### B.1 Nota metodológica

- **Escala 0-5 por criterio** (en pasos de 0,5). 0 es ausencia o "No evidenciado"; 1 es una mención mínima o genérica, solo comercial; 2 es cobertura parcial, solo comercial o de otra geografía; 3 es cobertura razonable, mayormente comercial; 4 es cobertura amplia y específica, con parte comprobada o muy detallada; 5 es cobertura completa y comprobada, nota que solo aparece una vez en la matriz (precio de DentalSoft). Esta descripción resume el criterio que aplican las fichas, que es de juicio del analista y no una fórmula.
- **Regla de evidencia (de la plantilla de las fichas).** Un ítem "No evidenciado" puntúa como ausente (no suma). Una afirmación comercial suma la mitad que una comprobada.
- **Las notas son conservadoras** porque casi toda la evidencia pública es comercial: no se infieren funciones que no figuren, y lo no publicado no suma. La regla de "mitad de puntaje" se aplicó como criterio de juicio y no como tope mecánico. Por ejemplo, DriCloud recibe 4,0 en turnos con todo el contenido como afirmación comercial. Por eso los totales son **preliminares**, y diferencias de 0,1 a 0,2 puntos no deben leerse como superioridad real.
- **Pesos:** gestión de turnos y automatización 25 %; funcionalidad odontológica clínica 20 %; integraciones locales y WhatsApp 15 %; administración, cobros y facturación 15 %; experiencia del paciente 10 %; seguridad, exportación y trazabilidad 10 %; precio y facilidad de adopción 5 %. Los pesos suman 100 %.
- **Total ponderado recalculado por el equipo de redacción** (suma de nota × peso). Las notas son las de la sección "Puntaje preliminar" de cada ficha. **No se halló ningún error aritmético**: todas las diferencias con el total de la ficha son de redondeo (las fichas redondean a un decimal, salvo Dentalink, Encuadrado y AgendaPro que usan dos; la mayor diferencia es 0,05). Por eso la matriz muestra el total exacto en tres decimales.
- **Relevancia para Argentina** (Alta/Media/Baja) tomada de cada ficha. Esta nota mide el producto en términos absolutos y no la adecuación al mercado argentino: un producto de Baja relevancia puede puntuar alto por su calidad general (por ejemplo Nubimed, Dentally o Curve Dental).

### B.2 Matriz

Columnas: T = Gestión de turnos y automatización (25 %); C = Funcionalidad odontológica clínica (20 %); I = Integraciones locales y WhatsApp (15 %); A = Administración, cobros y facturación (15 %); E = Experiencia del paciente (10 %); S = Seguridad, exportación y trazabilidad (10 %); P = Precio y facilidad de adopción (5 %). Orden: total recalculado, de mayor a menor.

| Pos. | Producto | Relevancia AR | T | C | I | A | E | S | P | **Total recalculado** | Total en ficha |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | DriCloud / XDentalCloud | Alta | 4,0 | 3,0 | 4,0 | 4,0 | 3,5 | 3,5 | 4,0 | **3,700** | 3,7 |
| 2 | DentalCore | Alta | 3,5 | 4,0 | 3,5 | 4,0 | 2,5 | 3,5 | 3,5 | **3,575** | 3,6 |
| 3 | Dentia | Alta | 3,5 | 3,5 | 3,5 | 3,5 | 3,0 | 3,5 | 4,5 | **3,500** | 3,5 |
| 4 | Open Dental | Media | 3,5 | 3,5 | 3,5 | 3,0 | 3,5 | 2,5 | 4,5 | **3,375** | 3,4 |
| 5 | Nubimed | Baja | 3,5 | 3,5 | 2,5 | 3,0 | 3,5 | 3,0 | 4,5 | **3,275** | 3,3 |
| 6 | Dentally | Baja | 3,5 | 3,5 | 2,5 | 3,0 | 3,5 | 3,5 | 3,0 | **3,250** | 3,3 |
| 7 | DentalSoft | Alta | 3,5 | 3,0 | 3,0 | 3,0 | 3,5 | 2,5 | 5,0 | **3,225** | 3,2 |
| 8 | Curve Dental | Baja | 3,5 | 3,5 | 2,0 | 3,5 | 3,5 | 3,0 | 1,5 | **3,125** | 3,1 |
| 9 | Clinicorp | Baja | 3,5 | 3,0 | 3,0 | 3,0 | 3,0 | 1,5 | 3,5 | **3,000** | 3,0 |
| 9 | Dentrix Ascend | Baja | 3,0 | 3,5 | 2,0 | 3,5 | 3,0 | 3,5 | 1,5 | **3,000** | 3,0 |
| 9 | Simples Dental | Baja | 3,5 | 3,5 | 2,5 | 3,0 | 3,5 | 1,0 | 3,0 | **3,000** | 3,0 |
| 12 | Clinic Cloud | Baja | 3,5 | 2,0 | 2,5 | 2,5 | 3,5 | 3,0 | 4,0 | **2,875** | 2,9 |
| 13 | Medicloud | Alta | 3,5 | 0,5 | 3,5 | 3,0 | 3,5 | 1,5 | 4,5 | **2,675** | 2,7 |
| 14 | Dentalink | Alta | 3,0 | 3,0 | 2,0 | 3,0 | 3,0 | 2,0 | 1,0 | **2,650** | 2,65 |
| 15 | DentalTec | Alta | 2,0 | 2,5 | 3,5 | 3,5 | 1,0 | 3,0 | 1,5 | **2,525** | 2,5 |
| 16 | Doctocliq | Media-Baja | 2,5 | 2,5 | 2,5 | 2,0 | 2,0 | 2,0 | 4,0 | **2,400** | 2,4 |
| 17 | Medilink | Baja | 3,0 | 1,0 | 2,0 | 2,5 | 2,5 | 1,5 | 1,0 | **2,075** | 2,1 |
| 18 | Calu | Media | 3,0 | 0,5 | 2,5 | 1,5 | 3,0 | 1,0 | 2,5 | **1,975** | 2,0 |
| 19 | Docturno | Media | 3,0 | 0,5 | 2,5 | 0,5 | 4,0 | 1,0 | 1,0 | **1,850** | 1,9 |
| 19 | Encuadrado | Baja | 3,0 | 0,5 | 1,5 | 2,0 | 2,5 | 0,5 | 3,5 | **1,850** | 1,85 |
| 21 | AgendaPro | Baja | 3,0 | 0,0 | 2,0 | 2,0 | 3,0 | 0,5 | 1,0 | **1,750** | 1,75 |
| 22 | Dentidesk | Baja | 2,0 | 2,5 | 1,0 | 1,5 | 1,0 | 1,0 | 2,0 | **1,675** | 1,7 |
| 23 | Gesden | Baja | 1,5 | 2,5 | 0,5 | 1,0 | 0,5 | 1,0 | 2,0 | **1,350** | 1,4 |

**Observaciones sobre la matriz.**

- Los **top 5** por total son DriCloud/XDentalCloud (3,700), DentalCore (3,575), Dentia (3,500), Open Dental (3,375) y Nubimed (3,275). De los cinco, solo Open Dental tiene evidencia comprobada relevante para agenda (reserva y ASAP); el resto se apoya en afirmaciones comerciales.
- Entre los productos de relevancia Alta, el mejor es DriCloud (3,700), aunque es de origen español y no evidencia obras sociales argentinas. Entre los de origen argentino, el mejor es DentalCore (3,575).
- DentalSoft obtiene la única nota 5 de la matriz (precio y adopción, por plan gratuito y demo sin tarjeta), con peso de solo 5 %.
- Los productos no odontológicos (Medicloud, Calu, Docturno, Encuadrado, AgendaPro, Medilink) quedan penalizados por el 20 % de peso clínico. Sirven como referencia de agenda y reserva, no como competidores directos.
- Gesden tiene la menor nota, pero su ficha es de **baja confianza**: ningún ítem pudo marcarse Comprobado porque el sitio oficial no estuvo accesible y todo proviene de terceros, varios de ellos competidores.

---

## C. Análisis competitivo

### C.1 Funcionalidades estándar de mercado

Se consideran estándar las funciones declaradas por la mayoría de los 23 sistemas. Los recuentos se obtuvieron de las celdas de A.1 y A.2 y consideran tanto afirmación comercial como comprobado.

| Funcionalidad | Cuántos la declaran o la muestran | Estado de la evidencia |
|---|---|---|
| Agenda con varios profesionales | 18 de 23 | Todo afirmación comercial; ninguna ficha la comprueba |
| Reserva online del paciente | 19 de 23 | Solo Open Dental la tiene documentada (Web Sched); Clinic Cloud la incluye en su plan; el resto es comercial |
| Recordatorios de turno (email, SMS o WhatsApp) | La gran mayoría | Comprobado como tarifa o plan en Open Dental, Nubimed y Clinic Cloud; el resto es comercial |
| Recordatorios o confirmaciones por WhatsApp | 17 de 23 | Todo comercial; en varios casos con costo variable aparte (Meta, paquetes de mensajes) |
| Odontograma o charting | Alrededor de 16 de 23 (los 6 sistemas no odontológicos no lo tienen y Dentrix Ascend no lo evidencia) | Comercial; en Clinic Cloud solo en el plan Max |
| Facturación fiscal local | Según país de origen | Es estándar en cada geografía (SII, NFS-e, VeriFactu, seguros de EE.UU.); en Argentina solo 5 sistemas declaran ARCA |
| Planes con precio publicado | 13 de 23 | Es la información más verificable; 10 sistemas no publican montos (uno de ellos, Gesden, solo por terceros) |

Conclusión: el estándar actual es **agenda multiprofesional + reserva online + recordatorios + odontograma + facturación local**. Ese conjunto no diferencia a nadie.

### C.2 Diferenciadores reales

**Comprobados** (figuran en documentación, tarifas o ayuda; siguen sin ser una prueba funcional):

- **Open Dental:** Web Sched con tres modos (paciente nuevo, existente y recall) y lista ASAP con enlace único por paciente para aceptar o rechazar el hueco; API REST documentada con recursos de Patient, Appointment y Schedule Operatories; tarifas publicadas completas, con tarifa reducida "Other Countries" y gratuita "Developing Countries". Si Argentina cae en una de esas categorías no está declarado.
- **Dentally:** modelo de precio por sillón ("surgery") con tres niveles, publicado.
- **DentalSoft:** documentación funcional pública por módulo (en especial la cobertura de obras sociales), plan gratuito y demo sin tarjeta.
- **Dentalink:** existencia de una colección de ayuda "Módulo exclusivo para Argentina" (obras sociales, conciliaciones, receta electrónica). Solo se leyeron los títulos; el contenido no está leído.
- **Nubimed, Clinic Cloud y DriCloud:** precios publicados con extras visibles, un elemento de transparencia poco común en el sector.

**Afirmaciones comerciales** (declaradas, sin demostración):

- **Asistentes de IA por WhatsApp** que agendan, reprograman y cancelan: Dentia, DentalSoft (recepcionista virtual), Doctocliq (Soyla), Clinicorp (Cloudia), Medilink (Contact Center IA), Simples Dental (secretaria IA), Dentalink (contact center IA) y Medicloud (bot, con costo aparte).
- **ARCA integrado:** DriCloud, DentalCore, Dentia, DentalTec y Medicloud. **Validación en línea con obras sociales:** solo DentalTec (más de 15 por SOAP); DentalCore aclara expresamente que no valida en línea.
- **Lista de espera:** DriCloud (aviso a todos, gana el primero), Curve Dental ("Smart Fill"), Nubimed, Dentally y Simples Dental (vía "encaje").
- **Canal de captación de pacientes:** Docturno (búsqueda por cobertura médica) y Clinic Cloud (puente con Doctoralia).
- **IA clínica:** análisis de radiografías y notas por voz (Dentia, Dentalink, Dentrix Ascend, Dentally, Open Dental).
- **Transparencia sobre límites:** DentalCore declara que no factura en nombre del profesional y que no valida en línea con obras sociales. Es una declaración, no una prueba.

### C.3 Vacíos frecuentes del mercado argentino

1. **El detalle de la agenda no se publica.** Solo 2 de 23 declaran prevención de solapamientos (DentalSoft y Simples Dental), 7 muestran gestión por sillón o box (DriCloud, Open Dental, Dentally, Curve Dental, Simples Dental, Clinic Cloud, Gesden; los dos primeros de forma indirecta), solo 3 mencionan duración variable (DriCloud, DentalTec, Docturno en categoría de ayuda) y 8 muestran bloqueos. Para un consultorio con varios sillones, esto es el núcleo del producto.
2. **Ningún sistema argentino evidencia sillones, duración variable y solapamientos a la vez.** Los productos de origen argentino (DentalCore, Dentia, DentalSoft, DentalTec, Medicloud) cubren lo fiscal y lo local; el modelo de sillón, que sí aparece en productos extranjeros (Dentally, Curve Dental, Simples Dental), no se evidencia en ellos.
3. **Reserva online del paciente no evidenciada en DentalCore, DentalTec ni Dentia** (Dentia ofrece un bot de WhatsApp, no una reserva propia), a pesar de ser productos con foco argentino.
4. **ARCA, obras sociales y WhatsApp sin comprobación.** Cinco sistemas declaran ARCA y ninguno lo comprueba. La validación de obras sociales es una afirmación de DentalTec y los artículos de ayuda de Dentalink no se pudieron leer. DentalSoft es el único con documentación funcional de obras sociales, y no evidencia ARCA.
5. **Política de cancelación.** Ninguna ficha documenta un plazo de cancelación configurable ni el registro de cancelaciones tardías. Es un vacío a partir de lo relevado, no una prueba de que ningún producto lo tenga.
6. **Lista de espera sin criterio de compatibilidad.** DriCloud notifica a todos y gana el primero; Open Dental envía un enlace ASAP; ninguna ficha describe que el sistema filtre por compatibilidad (profesional, prestación, duración).
7. **Precios.** Solo 4 sistemas publican precio en pesos (DentalCore, Dentia, DentalSoft sin código de moneda y Medicloud). DentalCore contradice su propia página (ARS en la sección de precios y USD en la FAQ). Dentalink, DentalTec (software), Curve Dental, Docturno, Dentrix Ascend y otros no publican montos.
8. **Cumplimiento de datos.** Solo Dentia declara tratar datos de salud conforme a la Ley 25.326 (afirmación comercial); DentalCore la menciona de forma inconsistente entre páginas; el resto no la menciona. DriCloud aloja datos en Europa, un punto a considerar por la transferencia internacional de datos de pacientes argentinos.
9. **Adopción sin respaldo.** Casi no hay reseñas verificables: los datos de clientes provienen de las propias landings, y los datos de Capterra solo se vieron en resultados de búsqueda.
10. **Exportación de datos.** Poco evidenciada. Nubimed la incluye en sus planes; DentalCore y Doctocliq mencionan plazos tras la baja; el resto no la evidencia.

### C.4 Oportunidades de innovación

Las oportunidades se derivan de los vacíos anteriores y son hipótesis de diseño, no hallazgos comprobados.

1. **Agenda por sillón con reglas explícitas y documentadas.** Publicar con claridad cómo se evitan los solapamientos, la duración por prestación y los bloqueos aprovecharía el vacío más amplio del mercado. Es el centro del MVP.
2. **Lista de espera con compatibilidad.** Proponer, al liberarse un hueco, solo pacientes compatibles con la duración, el profesional y la prestación. Ninguna ficha lo describe; DriCloud y Open Dental resuelven un problema parecido de forma más simple.
3. **Reserva y gestión del turno sin cuenta, por enlace único.** Open Dental lo comprueba para ASAP; DentalCore y Nubimed lo declaran para confirmar o cancelar. Extenderlo a ver, confirmar y cancelar reduce la fricción para el paciente.
4. **Política de cancelación visible y configurable** (cancelación sin penalidad hasta 24 hs, tardía registrada), un vacío en todas las fichas.
5. **Simplicidad para recepción.** Varias fichas muestran cientos de funciones y hasta 17 perfiles de rol (Dentia); un rol de recepción reducido es un posible diferencial de usabilidad.
6. **Transparencia comercial.** Publicar precios en ARS con moneda explícita y con costos adicionales visibles, algo que hoy hacen pocos (DentalCore, Dentia, Medicloud).
7. **Integraciones de pago y facturación como etapa posterior**, apoyadas en un diseño que ya contemple los datos necesarios (obra social como dato, paciente con identificación).

---

## D. Recomendación final

### D.1 Cinco competidores prioritarios para analizar mediante demo

Criterio de selección: puntaje, relevancia para Argentina, cobertura de lo que el MVP necesita (agenda, reserva, lista de espera) y **disponibilidad real de una prueba o demo** según las fichas.

1. **DriCloud / XDentalCloud** (3,700; Alta). Es el de mayor puntaje y el único con declaración conjunta de sillones, lista de espera con liberación de huecos, ARCA y Mercado Pago. Tiene enlace de demo (/acceder-demo/, condiciones no leídas). Se debe observar cómo resuelve bloqueos, sobreturnos y solapamientos, que no están evidenciados.
2. **DentalCore** (3,575; Alta). Es el líder entre los de origen argentino, con ARCA, 65 obras sociales cargadas y una propuesta de límites transparente. Ofrece prueba gratis de 7 días solicitada por WhatsApp. Conviene verificar si existe reserva online del paciente, el portal y los enlaces de confirmación y cancelación.
3. **Dentia** (3,500; Alta). Ofrece prueba gratuita de 30 días con todas las funciones. Permite comprobar el asistente de IA por WhatsApp que opera la agenda y si hay reserva propia, sillones y bloqueos, que no están evidenciados.
4. **DentalSoft** (3,225; Alta). Tiene plan gratuito y demo inmediata sin tarjeta. Es el único que declara detección automática de solapamientos junto con sobreturnos y bloqueos entre los de origen argentino, y tiene documentación funcional de obras sociales. Falta comprobar sillones y lista de espera.
5. **Open Dental** (3,375; Media). Es el único con reserva online, lista ASAP y API REST **documentadas** públicamente, con prueba descargable (https://www.opendental.com/site/trial.html, no leída). Sirve para comparar el estándar técnico y de UX de un producto maduro.

**Reservas:** DentalTec (validación en línea con obras sociales) y Dentalink (módulo Argentina) son valiosos, pero solo ofrecen demo a solicitud y sin condiciones descritas; quedan como siguiente prioridad. Nubimed y Dentally puntúan alto, pero son de relevancia Baja por origen y facturación.

### D.2 Tres productos de referencia para experiencia de usuario

1. **Open Dental (Web Sched y ASAP).** Es la única referencia comprobada: reserva en tres modos y enlace único por paciente para aceptar o rechazar un hueco liberado. Es la base del flujo de reserva sin login por token y de la lista de espera del MVP.
2. **Dentally (portal del paciente).** Según su ficha, ofrece reserva, formularios online, tablet "Concierge" y kiosk de check-in, y publica un modelo de precio por sillón. La evidencia es comercial. Es una referencia de portal, no una función comprobada.
3. **AgendaPro (reserva estilo e-commerce).** Su ficha lo marca como referente de experiencia de reserva: sitio de reservas 24/7, pago con enlace y Reserve with Google. Es un producto horizontal sin función clínica dental y la evidencia es comercial.

Alternativa: **Curve Dental** (texto bidireccional, "Smart Fill" y pedido de reseñas) como referencia de engagement del paciente.

### D.3 MVP

Las decisiones del MVP las tomó el equipo; se presentan tal cual y vinculadas con la evidencia relevada. Los recuentos se refieren a los 23 sistemas.

#### Imprescindible

| Función del MVP | Evidencia en el mercado (fichas) | Comentario |
|---|---|---|
| **Agenda por profesional y por sillón/box con prevención de solapamientos.** Sin sobreturnos. Un turno que empieza justo cuando termina otro no es un solapamiento. | Sillones/boxes: 7 de 23 (DriCloud, Open Dental y Dentally indirectamente; Curve Dental, Simples Dental, Clinic Cloud y Gesden como afirmación). Prevención de solapamientos: 2 de 23 como afirmación comercial (DentalSoft y Simples Dental). Sobreturnos: afirmación comercial en 4 (DentalSoft, Dentia, Medicloud, Simples Dental) | Es el vacío más amplio del mercado. No admitir sobreturnos es una decisión del equipo que simplifica la regla; ninguna ficha la documenta. |
| **Duración por prestación (ajustable).** | DriCloud (afirmación comercial: duración predefinida por tratamiento, arrastrable); DentalTec (afirmación: intervalos personalizables); Docturno (comprobado solo como categoría de ayuda) | Solo 3 de 23; poco publicado. |
| **Bloqueos de agenda y horarios por profesional.** | Bloqueos: 8 aproximadamente (DentalCore, DentalSoft, Curve Dental, Simples Dental, Encuadrado, Open Dental, Medilink, Docturno). Horarios individuales por profesional cruzados con los de la clínica: DentalSoft (afirmación) | Función habitual en el mercado, aunque con poco detalle. |
| **Crear, reprogramar y cancelar turnos.** Cancelación sin penalidad hasta 24 hs antes (configurable); fuera de plazo se registra como tardía. | Reprogramación: Dentalink, Dentia (bot), Dentidesk, Encuadrado, todas como afirmación. Plazo de cancelación configurable y registro de tardías: **no aparece en ninguna ficha** | Es un diferencial de reglas de negocio frente a lo relevado. |
| **Reserva online del paciente sin login**, representado por un token o enlace único para ver, confirmar o cancelar. | Open Dental: Web Sched y enlace ASAP único por paciente (comprobado). DentalCore: confirmar o cancelar con enlace que libera el horario (afirmación). Nubimed: confirmar o cancelar desde el recordatorio (afirmación). DentalSoft: reserva por 4 pasos con DNI (afirmación) | El token evita pedir cuenta o DNI y sigue el patrón comprobado de Open Dental. |
| **Ficha básica de paciente** con datos de contacto, obra social como dato y notas. | DentalSoft y DentalCore tratan la obra social como dato con cobertura asociada; Medicloud permite múltiples coberturas por paciente | La cobertura, el copago y la liquidación quedan para etapas posteriores. |
| **Recordatorio por email real (SMTP).** | Email de recordatorios: Open Dental (comprobado en tarifas), Nubimed (comprobado como tarifa, email gratis), DriCloud (gratis), Medicloud (gratis: recordatorios por email y turnos fijos 48 h antes) | El canal más simple y sin costo variable, a diferencia de WhatsApp (Meta cobra por mensaje en DentalSoft). |
| **Roles: odontólogo (eje del producto), recepción (lo más simple posible) y administrador.** | Roles múltiples: DentalCore 4 roles, DentalTec 10, Dentia 17, DentalSoft permisos por rol, Clinicorp niveles de acceso, Dentrix Ascend RBAC (todo afirmación) | El mercado ofrece más roles de los que necesita un MVP. |

#### Diferenciador

| Función | Evidencia | Comentario |
|---|---|---|
| **Lista de espera que propone pacientes compatibles al liberarse un hueco.** | Lista de espera en 6 de 23: Open Dental (ASAP, comprobado), DriCloud (aviso a todos; gana el primero), Curve Dental ("Smart Fill"), Nubimed, Dentally y Simples Dental (afirmación). Ninguna ficha describe filtro por compatibilidad | Es la oportunidad central C.4.2. Se apoya en la agenda por sillón y la duración por prestación, que el MVP ya incluye. |

#### Etapas posteriores

| Función | Evidencia y motivo del aplazamiento |
|---|---|
| **Odontograma, periodontograma e historia clínica completa** | Odontograma en cerca de 16 de 23 y periodontograma en 8 (DriCloud, DentalCore, Dentia, Dentalink, Nubimed, Dentally, Curve Dental, Gesden), todo comercial. Es amplio y de gran peso (20 %), pero no es el eje del primer producto. |
| **WhatsApp** | Declarado en 17 de 23, todo comercial y con costo variable de Meta o paquetes de mensajes (DentalSoft, DentalTec, Medicloud). Requiere API oficial. |
| **Mercado Pago y señas** | Mercado Pago en 7 (DriCloud, DentalCore, Dentia, DentalSoft, Medicloud, Docturno, Calu); señas en Dentia (Elite) y DentalSoft (Pro); turnos con pago obligatorio en Medicloud (PRO). Todo comercial. |
| **Facturación ARCA** | Declarada en 5 (DriCloud, DentalCore, Dentia, DentalTec, Medicloud), ninguna comprobada. Exige certificado y CUIT del consultorio (DentalCore). |
| **Validación online de obras sociales** | Solo DentalTec lo declara (más de 15 por SOAP); DentalCore aclara que no valida en línea. Requiere convenios y servicios de terceros. |
| **Multisede** | Declarada por la mayoría de los productos de mayor puntaje (DentalCore, Dentia, DentalSoft, DriCloud, Clinic Cloud, Dentally, Dentrix Ascend, entre otros). Sin evidencia comprobada. |
| **Campañas y recuperación de pacientes** | DentalSoft (Pro), Dentalink, DentalCore (recall por segmentos), Dentia (Elite), Clinicorp, Curve Dental. Todo comercial. |
| **Asistentes con IA** | Dentia, DentalSoft, Doctocliq, Clinicorp, Medilink, Simples Dental, Dentalink, Medicloud. Todo comercial y de costo variable. |

---

## Verificación de fuentes

> ⚠️ **ESTA SECCIÓN ES UN EJEMPLO / BORRADOR DE BASE.** La consigna exige que **cada integrante abra y compruebe personalmente** las fuentes. El agente abrió estas 6 URLs el 2026-11-20 en un navegador real, leyó el texto de cada página y completó la tabla **solo como modelo de redacción**. El equipo tiene que repetir la verificación por su cuenta, reemplazar cada fila con lo que encuentre (o confirmar lo que coincide), poner quién verificó y en qué fecha, y borrar este aviso. Una verificación hecha por el agente **no** cuenta como verificación personal.

| # | Tipo de fuente | URL | Qué afirma el informe | Qué se encontró (EJEMPLO, 2026-11-20) | Corrección aplicada (EJEMPLO) | Verificó / fecha |
|---|---|---|---|---|---|---|
| 1 | Página de precios (DentalCore) | https://dentalcore.app/software-dental/argentina y https://dentalcore.app/ | El plan único cuesta $75.000/mes (ARS) con 2 usuarios y $15.000 por usuario adicional; la FAQ indica USD 50/mes. No se sabe cuál está vigente. | **Coincide en parte.** La página de Argentina dice "UN SOLO PLAN, CON TODO $ 75.000 por mes", con 2 usuarios y $ 15.000 por persona más, pago en pesos. La home, en cambio, **muestra el precio según el país detectado**: desde nuestra conexión ofreció "¿Estás en Colombia?" y un precio de "COP 167.000 por mes". La discrepancia con USD 50 se explica porque el precio es **geolocalizado**, no por un error. | El precio para Argentina queda como **Comprobado: $ 75.000/mes (ARS)** con la página `/software-dental/argentina` como fuente. Se agrega la nota "el sitio muestra precios distintos según el país". Se descarta el USD 50 como precio para Argentina. | _(nombre)_ / _(fecha)_ |
| 2 | Página legal / cumplimiento (DentalCore) | https://dentalcore.app/software-dental/argentina y https://dentalcore.app/privacidad | La home menciona la Ley 25.326, pero la página de Argentina no; también se cita la Ley 26.529. | **Corregido.** La página de Argentina **no** menciona la 25.326, pero sí la **Ley 26.529** (historia clínica inalterable, consentimiento informado, art. 7). La **política de privacidad** sí cita el "artículo 12 de la Ley N° 25.326", la AAIP como autoridad de control y un código de registro en el Registro Nacional de Bases de Datos Personales. En la home no se encontró la 25.326. | La referencia a la Ley 25.326 se mueve de "home" a "política de privacidad (/privacidad)". El cumplimiento declarado queda como **Afirmación comercial con respaldo documental** (registro ante la AAIP publicado), no como funcionalidad comprobada. | _(nombre)_ / _(fecha)_ |
| 3 | Centro de ayuda (Dentalink) | https://ayuda.softwaredentalink.com/es/collections/19196578-modulo-exclusivo-para-argentina | Existe un "Módulo exclusivo para Argentina" con 4 artículos (obras sociales, conciliaciones y receta electrónica); no se leyó el cuerpo. | **Coincide.** La colección existe ("Encontrarás todos los detalles de Dentalink para Argentina", 4 artículos). Al abrir "¿Como crear una obra social?" (27/03/2026) el cuerpo **no tiene texto**: el contenido es un **video de YouTube embebido** y capturas. En la colección no aparece ARCA/AFIP. | Obras sociales queda como **Comprobado (existencia de documentación de ayuda con video oficial)**, con la aclaración de que no se vio el video. ARCA sigue **No evidenciado**. La relevancia de Dentalink para Argentina conviene bajarla de "Alta" a "Media". | _(nombre)_ / _(fecha)_ |
| 4 | Sitio oficial (DriCloud / XDentalCloud) | https://www.dricloud.com/facturacion-electronica/ | Factura electrónica ante ARCA (A/B/C con CAE) e integración con Mercado Pago, todo como afirmación comercial. | **Coincide.** La página declara facturación en 7 países, incluida "Argentina ARCA (ex AFIP) · Factura electrónica con CAE": CAE por comprobante y punto de venta, Factura C para monotributistas y A o B para responsables inscriptos, y notas de crédito. Lista "Square, Stripe, PayPal, MercadoPago o Bitcoin, integrados". No muestra el producto funcionando. | Sin cambios: se mantiene como **Afirmación comercial**. Se agrega el detalle de tipos de comprobante para Argentina. | _(nombre)_ / _(fecha)_ |
| 5 | Documentación técnica / tarifas (Open Dental) | https://www.opendental.com/site/fees.html | Existen las tarifas "Other Countries" (US$89/mes por ubicación) y "Developing Countries" (gratis con soporte limitado); no se sabe si Argentina entra en alguna. | **Coincide.** "Other Countries: $89/month/location or office", hasta 3 profesionales, soporte por email y teléfono, contrato de 12 meses. "Developing Countries: Free", con soporte limitado y ejemplos "(India, Kenya, Cambodia, etc.)". La página **no menciona Argentina** ni Latinoamérica. | Se mantiene US$89/mes como **Comprobado** para "Other Countries". Si Argentina califica como "Developing Countries" queda **No evidenciado** y pasa a preguntas abiertas. | _(nombre)_ / _(fecha)_ |
| 6 | Marketplace / reseñas (Capterra, Dentalink) | https://www.capterra.es/software/196361/dentalink | "Precio inicial 29,00 US$", visto solo en un snippet de búsqueda; contradicción sobre la prueba gratuita. | **Coincide.** La página carga en capterra.es y muestra "Precio inicial 29,00 US$ mes" y "Prueba gratuita", con Argentina entre los "Países disponibles". Tiene **solo 2 opiniones** (5,0). El proveedor figura "Ubicado en Santiago de Chile, Chile. Fundada en 2009". | El precio pasa a **Comprobado en marketplace** (precio declarado en Capterra, no en el sitio del proveedor). Se agrega que 2 opiniones **no son evidencia suficiente de adopción**. | _(nombre)_ / _(fecha)_ |

**Resumen del ejemplo:** de 6 fuentes revisadas, 4 coinciden con el informe, 1 se corrigió (la ubicación de la Ley 25.326 en DentalCore) y en 1 se encontró una causa nueva para una inconsistencia (precio geolocalizado de DentalCore). El hallazgo más útil para la reflexión es que **una herramienta automática puede leer una página distinta a la que ve un usuario de otro país**: el mismo dominio muestra precios diferentes según la ubicación.

Otros datos dudosos para verificar en una segunda pasada (sin precargar resultados): la discrepancia de precio de Clinicorp entre la landing (Standard R$ 149,90) y la página de planes (R$ 159,90); los precios de Simples Dental con montos mensuales adicionales sin modalidad; la inconsistencia de DriCloud (510 €/año frente a 12 × 42 €); el uptime de Dentrix Ascend (99,99 % en la home y 99,99987 % en la FAQ); las notas de Gesden, hechas solo con fuentes de terceros; y los contadores de las páginas de Clinic Cloud y DentalTec, que renderizan "0+".

### Limitaciones de la investigación

1. **Sitios no accesibles o descartados.** No se pudo acceder a Doctoralia Argentina, al sitio oficial de Gesden/Infomed (conexión rechazada en www.infomed.es y www.gesden.es; gesden.com corresponde a otra empresa no relacionada), a ZYRA ni a AhoraMiTurno. Reservo se descartó. Ninguno de estos cuatro cuenta con ficha; la ficha de Clinic Cloud solo aporta que Doctoralia se declara presente en 13 países, incluida Argentina. Tampoco pudo verificarse https://www.dentally.co (bloqueado en el fetch).
2. **Gesden se analizó solo con terceros.** Las fuentes son un competidor (Pabau) y una comparativa externa; por eso ningún ítem pudo marcarse Comprobado y la ficha es de baja confianza.
3. **Datos obtenidos solo de buscadores.** Los montos y calificaciones de Capterra de Dentalink (precio inicial), Curve Dental (4,4 con 285 reseñas), Dentrix Ascend (4,1 con unas 235), Open Dental (4,6 con 87) y Dentally (una reseña de 1,0/5 mencionada) se vieron solo en resultados de búsqueda. capterra.com devolvió 403 en acceso directo (Dentalink, Dentally). La prensa sobre Doctocliq, la ficha de Docturno en el sitio de Grupo Cormos y varias menciones de terceros también provienen de snippets de búsqueda.
4. **WebFetch devuelve extractos resumidos, no texto literal.** Las fichas se redactaron a partir de extracciones automáticas de los sitios, lo que puede omitir matices, contenido renderizado por JavaScript (contadores en "0+" en Clinic Cloud y DentalTec) o información en imágenes (por ejemplo, la tabla de planes de Dentally, parcialmente incierta para Starter y Essentials). Los datos no son citas textuales y deben verificarse en el sitio.
5. **Páginas listadas pero no leídas.** Muchas subpáginas (centros de ayuda de Medilink, Simples Dental y Dentidesk; políticas de privacidad y términos de Calu, DentalSoft, Dentia, Nubimed, Docturno; casos de éxito de DentalTec; módulos de DentalCore; FAQ de Clinic Cloud y Curve Dental; artículos de ayuda de Dentalink) se detectaron, pero no se leyeron. La ausencia de un dato en la ficha no prueba que el producto no lo tenga.
6. **Ningún producto se probó.** No se usaron demos ni pruebas gratuitas, no se accedió a ninguna aplicación y no se contactó a ningún proveedor.
7. **Casi toda la información es declarada por el propio proveedor.** Hay pocas reseñas verificables y las cifras de adopción son afirmaciones comerciales. Las "Comprobado" de las fichas se refieren a documentación o tarifas publicadas.
8. **Moneda y origen inciertos.** Varios sitios muestran "$" sin código de moneda (DentalSoft, Encuadrado, Calu, Dentrix Ascend) o no declaran el país de origen (Calu, Medilink, AgendaPro). En esos casos el informe lo indica y no deduce.
9. **Fecha única de consulta.** Todas las páginas se consultaron el 2026-11-20; los precios y funciones pueden cambiar.

---

## Referencias completas

Todas las URLs citadas en las fichas, agrupadas por sistema. Todas fueron **consultadas el 2026-11-20**. Cuando una URL no pudo leerse, se indica entre paréntesis. Las rutas relativas citadas en las fichas se completan con el dominio del sistema.

**1. AgendaPro**
- https://www.agendapro.com — sitio oficial
- https://www.agendapro.com/co/planes — página de planes (404 al consultar)

**2. Calu**
- https://www.calu.app/ — sitio oficial
- https://www.calu.app/planes — página de precios
- https://calu.app/terminos-y-condiciones — página legal (no leída)
- https://calu.app/politicas-de-privacidad — página legal (no leída)

**3. Clinic Cloud (Doctoralia)**
- https://www.clinic-cloud.com/ — sitio oficial
- https://www.clinic-cloud.com/tarifas — página de precios
- https://www.clinic-cloud.com/agenda — sitio oficial (agenda)
- https://www.clinic-cloud.com/faq — FAQ oficial (listada, no leída)
- https://pro.doctoralia.es/casos-exito?tag=clinic-cloud — casos de éxito (listada, no leída)

**4. Clinicorp**
- https://clinicorp.com — sitio oficial
- https://www.clinicorp.com/planos — página de precios
- https://www.clinicorp.com/gestao-financeira-clinipay — sitio oficial (módulo financiero)
- https://www.clinicorp.com/software-para-franquias-e-redes-de-odontologia-e-estetica — sitio oficial (franquicias)
- https://www.clinicorp.com/faq — FAQ oficial (indicada, no leída)

**5. Curve Dental**
- https://www.curvedental.com/ — sitio oficial
- https://www.curvedental.com/dental-scheduling-software — sitio oficial (agenda)
- https://www.curvedental.com/dental-patient-engagement-software — sitio oficial (engagement)
- https://www.curvedental.com/pricing — página de precios (sin cifras)
- https://www.curvedental.com/faq — FAQ oficial (citada en la home, no leída)
- https://www.curvedental.com/mobile — sitio oficial (app, listada)
- https://www.curvedental.com/partner-list — lista de partners (no leída)
- https://www.capterra.com/p/98688/Curve-Dental-Hero/reviews/ — marketplace (solo vía búsqueda)

**6. DentalCore**
- https://dentalcore.app/ — sitio oficial
- https://dentalcore.app/software-dental/argentina — sitio oficial (página país)
- https://dentalcore.app/blog/mejor-software-dental-argentina — blog del proveedor (no leída)
- https://www.capterra.com/p/10047210/Dentalcore/ — marketplace (no leída)

**7. Dentalink**
- https://web-es.softwaredentalink.com/en — sitio oficial
- https://web-es.softwaredentalink.com/planes — página de planes
- https://ayuda.softwaredentalink.com/es/collections/19196578-modulo-exclusivo-para-argentina — centro de ayuda (solo títulos)
- https://ayuda.softwaredentalink.com/es/articles/14744244-receta-electronica-argentina — centro de ayuda (cuerpo no accesible)
- https://ayuda.softwaredentalink.com/es/articles/14304573-como-recaudar-un-pago-con-obra-sociales — centro de ayuda (cuerpo no accesible)
- https://www.capterra.es/software/196361/dentalink — marketplace (solo vía búsqueda)
- https://www.capterra.com/p/196361/Dentalink/ — marketplace (403, calificación no leída)

**8. Dentally**
- https://www.dentally.com/en-gb — sitio oficial
- https://www.dentally.com/en-gb/pricing — página de precios
- https://www.dentally.com/en-gb/dental-software-features/clinical — sitio oficial (clínica)
- https://www.dentally.com/en-gb/dental-software-features/practice-admin — sitio oficial (administración)
- https://www.dentally.com/en-gb/product/dentally-portal — sitio oficial (portal)
- https://www.dentally.com/en-gb/product/dentally-vision — sitio oficial (imagen, listada)
- https://www.dentally.com/en-gb/security — sitio oficial (seguridad)
- https://www.dentally.com/en-gb/security-policy — página legal (no leída)
- https://www.dentally.com/en-gb/cloud-based-dental-software — sitio oficial (listada)
- https://www.dentally.com/en-gb/solutions/nhs — sitio oficial (listada)
- https://www.dentally.com/en-gb/partners — sitio oficial (listada)
- https://www.dentally.co — sitio alternativo (bloqueado en el fetch)
- https://www.capterra.com/p/153816/Dentally/ — marketplace (403)
- https://www.capterra.co.uk/software/153816/dentally — marketplace (aparece en búsqueda, no leída)
- https://www.applytosupply.digitalmarketplace.service.gov.uk/g-cloud/services/293077111384253 — marketplace de gobierno del Reino Unido (vía búsqueda)

**9. DentalSoft**
- https://dentalsoft.com.ar/ — sitio oficial
- https://dentalsoft.com.ar/demo — demo sin tarjeta
- https://www.dentalsoft.com.ar/funcionalidades/obras-sociales — documentación de producto
- https://www.dentalsoft.com.ar/funcionalidades/ — documentación de producto (índice)
- https://www.dentalsoft.com.ar/funcionalidades/agenda-odontologica — documentación de producto (listada)
- https://www.dentalsoft.com.ar/funcionalidades/turnos-online — documentación de producto (listada)
- https://www.dentalsoft.com.ar/funcionalidades/asistente-virtual-whatsapp — documentación de producto (listada)
- https://www.dentalsoft.com.ar/funcionalidades/caja-cobros — documentación de producto (listada)
- https://www.dentalsoft.com.ar/funcionalidades/cobros-mercadopago — documentación de producto (listada)
- https://www.dentalsoft.com.ar/funcionalidades/historia-clinica-odontograma — documentación de producto (listada)
- https://www.dentalsoft.com.ar/funcionalidades/ortodoncia — documentación de producto (listada)
- https://www.dentalsoft.com.ar/funcionalidades/pagina-web-odontologos — documentación de producto (listada)
- https://www.dentalsoft.com.ar/funcionalidades/presupuestos — documentación de producto (listada)
- https://www.dentalsoft.com.ar/funcionalidades/recuperacion-de-pacientes — documentación de producto (listada)
- https://www.dentalsoft.com.ar/privacidad — página legal (no leída; la ficha cita también /terminos y /eliminacion-datos del mismo dominio)
- https://www.dentalsoft.com.ar/blog/como-migrar-pacientes-excel-papel-sistema-odontologico — blog (no leída en detalle)

**10. DentalTec**
- https://web.dentaltec.com.ar/ — sitio oficial
- https://web.dentaltec.com.ar/funcionalidades — sitio oficial (funcionalidades)
- https://web.dentaltec.com.ar/preguntas-frecuentes — FAQ oficial
- https://web.dentaltec.com.ar/casos-de-exito — casos de éxito (no leída)
- https://youtube.com/playlist?list=PL4MN1RxFkCQqzCEsz2TGIlTROOwmLFdBG — tutoriales en video (no revisados)

**11. Dentia**
- https://dentia.tech/ — sitio oficial
- https://dentia.tech/#precios — sección de precios

**12. Dentidesk**
- https://www.dentidesk.cl — sitio oficial

**13. Dentrix Ascend**
- https://www.dentrixascend.com/ — sitio oficial
- https://www.dentrixascend.com/dental-solutions/marketing-and-patient-experience/simplify-patient-booking/ — sitio oficial (reserva)
- https://www.dentrixascend.com/dental-solutions/dentrix-ascend-packages-and-suites/ — sitio oficial (paquetes)
- https://www.dentrixascend.com/about-us/why-dentrix-ascend/ — sitio oficial (seguridad y operación)
- https://www.dentrixascend.com/dental-solutions/charting-imaging-and-clinical-ai/ — sitio oficial (clínica, citada)
- https://www.dentrixascend.com/dental-solutions/dental-insurance-billing-and-collections/ — sitio oficial (facturación, citada)
- https://www.henryscheinone.com/dental-solutions/api-exchange/ — sitio oficial del proveedor (citada)
- https://www.capterra.com/p/177828/Dentrix-Ascend/ — marketplace (solo vía búsqueda)
- https://www.capterra.com/p/177828/Dentrix-Ascend/pricing/ — marketplace (no leída)

**14. Doctocliq**
- https://www.doctocliq.com — sitio oficial
- https://www.doctocliq.com/planes-y-precios — página de precios
- https://infomercado.pe/doctocliq-el-software-creado-por-tres-peruanos-ahora-esta-presente-en-12-paises-290922-cch/ — prensa (solo vía búsqueda)
- https://infomercado.pe/doctocliq-gana-concurso-de-startup-peru-y-llevara-tecnologia-a-mas-de-1000-consultorios-170622-mc/ — prensa (solo vía búsqueda)
- https://apps.apple.com/us/app/doctocliq/id1445399223 — tienda de aplicaciones (vía búsqueda, no leída)

**15. Docturno (Grupo Cormos)**
- https://www.docturno.com — sitio oficial
- https://profesionales.docturno.com/ — página de profesionales
- https://ayuda.cormos.com.ar/docturno — centro de ayuda
- https://www.docturno.com/privacidad — página legal (no leída; la ficha cita también /terminos-y-condiciones y /calidad)
- https://mercado.com.ar/protagonistas/grupo-cormos-presenta-crontu-una-nueva-plataforma-de-conexion-entre-pacientes-y-profesionales — prensa

**16. DriCloud / XDentalCloud**
- https://www.dricloud.com/ — sitio oficial
- https://xdentalcloud.com/ — sitio oficial (versión dental)
- https://xdentalcloud.com/agenda-dental/ — sitio oficial (agenda)
- https://www.dricloud.com/facturacion-electronica/ — sitio oficial (facturación ARCA)
- https://www.dricloud.com/seguridad/ — sitio oficial (seguridad)
- https://www.dricloud.com/lista-de-espera/ — sitio oficial (lista de espera)
- https://app.dricloud.net/precios/ — precios (redirige a aviso de login, sin precios)

**17. Encuadrado**
- https://www.encuadrado.com — sitio oficial

**18. Gesden (G5 / Gesden ONE)**
- https://www.infomed.es/ — sitio oficial (conexión rechazada, no leído)
- https://www.gesden.es/ — sitio oficial (conexión rechazada, no leído)
- https://pabau.com/es/blog/alternativas-a-gesden/ — artículo de competidor (sesgado)
- https://pabau.com/es/blog/precios-de-infomed/ — competidor (solo vía búsqueda)
- https://hackceleration.com/es/labs/mejor/software-gestion-clinica-dental — comparativa de terceros (vía búsqueda)
- https://apps.apple.com/us/app/-/id1068545222 — tienda de aplicaciones (no leída)
- https://www.ilerna.es/es/curso-gesden-1152 — curso de formación (listada en búsqueda, no leída)

**19. Medicloud**
- https://www.medicloud.com.ar — sitio oficial
- https://www.medicloud.com.ar/precios — página de precios

**20. Medilink**
- https://www.softwaremedilink.com — sitio oficial
- https://www.softwaremedilink.com/planes — página de planes
- https://www.softwaremedilink.com/gestion-de-pacientes/agendamiento-online — sitio oficial (agendamiento)

**21. Nubimed**
- https://www.nubimed.com/ — sitio oficial
- https://www.nubimed.com/planes-precios/ — página de precios
- https://www.nubimed.com/software-clinica-dental/ — sitio oficial (módulo dental)
- https://www.nubimed.com/privacidad-y-condiciones/ — página legal (listada, no leída)

**22. Open Dental**
- https://www.opendental.com/ — sitio oficial
- https://www.opendental.com/site/fees.html — página de precios y tarifas
- https://www.opendental.com/site/order.html — precios y contrato
- https://www.opendental.com/site/websched.html — documentación de servicio
- https://www.opendental.com/site/eservicesbundle.html — sitio oficial (eServices)
- https://opendental.com/site/apispecification.html — documentación técnica (API)
- https://opendental.com/site/license.html — cambio de licencia (solo vía búsqueda)
- https://www.opendental.com/site/trial.html — prueba descargable (listada, no leída)
- https://www.opendental.com/resources/OpenDentalAPI.pdf — documentación técnica (aparece en búsqueda, no leída)
- https://www.capterra.com/compare/2329-122350/Dentrix-vs-Open-Dental — marketplace (solo vía búsqueda)

**23. Simples Dental**
- https://www.simplesdental.com — sitio oficial (página Black Friday)
- https://www.simplesdental.com/agenda-online — sitio oficial (agenda)
- https://www.simplesdental.com/planos-e-precos — página de precios
- https://ajuda.simplesdental.com — centro de ayuda (indicado, no leído)
