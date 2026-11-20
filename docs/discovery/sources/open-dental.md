# Open Dental — Open Dental Software, Inc.

- **País de origen:** Estados Unidos (precios USD, teléfono de EE.UU.; tarifas por país) — Comprobado (https://www.opendental.com/site/fees.html)
- **URL oficial:** https://www.opendental.com/
- **Fecha de consulta:** 2026-11-20
- **Relevancia para Argentina:** Media — tiene tarifa explícita "Other Countries" (US$89/mes/ubicación) y "Developing Countries" (gratis con soporte limitado); tiene API REST documentada. Soporte en español, ARCA, Mercado Pago y presencia en Argentina: No evidenciado. Sitio solo en inglés. Si Argentina cae en "Developing Countries" no está declarado.

> Leyenda de estado por ítem: **Comprobado** (lo muestra el producto, documentación, centro de ayuda o video oficial — citar URL) · **Afirmación comercial** (lo dice la landing/marketing sin demostrarlo — citar URL) · **No evidenciado** (no se encontró respaldo público). Nunca dejar celdas vacías ni deducir.
> Nota del analista: páginas de documentación/tarifas oficiales = Comprobado; texto de landing = Afirmación comercial. Corrección importante: Open Dental ya NO es open source (cambió de GPL a licencia propietaria desde v24.4).

## 1. Segmento objetivo
| Segmento | Estado | Fuente |
|---|---|---|
| Consultorios dentales "de cualquier tamaño" | Afirmación comercial | https://www.opendental.com/ |
| Multi-ubicación (tarifa por volumen 4-9 y 10+ ubicaciones) | Comprobado | https://www.opendental.com/site/fees.html |
| Usuarios institucionales / integraciones médicas ($110/mes/FTE) | Comprobado | https://www.opendental.com/site/fees.html |
| Laboratorio, centro multidisciplinario | No evidenciado | — |

## 2. Modalidad
Software de escritorio con base de datos (.NET Framework) con eServices en la nube (Web Sched, portal, texting, ODMobile); versión cloud/self-hosted no declarada en la home — Afirmación comercial (https://www.opendental.com/). Apps: ODMobile, ODTouch, eClipboard — Afirmación comercial (idem). Prueba descargable: https://www.opendental.com/site/trial.html (listada, no leída).

## 3. Gestión de agenda
| Ítem | Estado | Fuente |
|---|---|---|
| Vista diaria/semanal | No evidenciado en páginas leídas | — |
| Múltiples profesionales | Afirmación comercial (mejora de "provider schedule setup" en v26.3 beta) | https://www.opendental.com/ |
| Múltiples sillones (operatorios) | Comprobado indirectamente: recursos de API "Schedule Operatories" | https://opendental.com/site/apispecification.html |
| Duración variable | No evidenciado | — |
| Bloqueos (blockouts) | Afirmación comercial (mejoras v26.3 beta; ASAP agrega blockout) | https://www.opendental.com/ ; https://www.opendental.com/site/websched.html |
| Sobreturnos | No evidenciado | — |
| Prevención de solapamientos | No evidenciado | — |

## 4. Turnos digitales
| Ítem | Estado | Fuente |
|---|---|---|
| Reserva online (Web Sched: Nuevo paciente, Paciente existente, Recall) | Comprobado (página de documentación del servicio) | https://www.opendental.com/site/websched.html |
| Enlace de reserva / sitio web | Comprobado (paciente nuevo reserva desde el sitio y se crea la ficha) | https://www.opendental.com/site/websched.html |
| Lista de espera "ASAP" (enlace único por paciente para aceptar/rechazar el hueco) | Comprobado | https://www.opendental.com/site/websched.html |
| Confirmación (eConfirmations) | Afirmación comercial | https://www.opendental.com/site/eservicesbundle.html |
| Texting integrado | Comprobado (tarifas) | https://www.opendental.com/site/fees.html |
| WhatsApp, redes, chatbot | No evidenciado | — |
| Cancelación / reprogramación por paciente | No evidenciado | — |

## 5. Automatización
eReminders por email, mensajes de agradecimiento automáticos, mensajes generales, mensaje-para-pagar (incluidos en soporte) — Comprobado (https://www.opendental.com/site/fees.html). Recordatorios de recall por link manual o automático; texto requiere Integrated Texting — Comprobado (https://www.opendental.com/site/websched.html). Mass Email — Comprobado en tarifa (https://www.opendental.com/site/fees.html). Seguimiento de ausentes, campañas segmentadas: No evidenciado.

## 6. Gestión odontológica
Odontograma gráfico 3D ("patient dentition and treatment over time"), recetas electrónicas, formularios digitales, planes de descuento — Afirmación comercial (https://www.opendental.com/). Análisis de imágenes con IA (US$149/mes/ubicación) — Comprobado en tarifa (https://www.opendental.com/site/fees.html). Periodontograma, presupuestos, anamnesis: No evidenciado en las páginas leídas.

## 7. Administración
Portal de pagos y mensaje-para-pagar incluidos en soporte — Comprobado (https://www.opendental.com/site/fees.html). 20+ clearinghouses (seguros EE.UU.), reportes y consultas — Afirmación comercial (https://www.opendental.com/). Mercado Pago, ARCA, obras sociales: No evidenciado.

## 8. Integraciones
API REST (GET/POST/PUT/DELETE; recursos Patient, Appointment, Schedule Operatories, etc.), separada de la API FHIR; requiere "Open Dental API Service" desde v24.3.61/24.4.33/25.1.2 — Comprobado (https://opendental.com/site/apispecification.html). Documentación PDF y listado de versiones FHIR en https://www.opendental.com/resources/OpenDentalAPI.pdf (aparece en resultados de búsqueda, no leído). "100s" de puentes (bridges) a otros programas — Afirmación comercial (https://www.opendental.com/). Tiers de precio para desarrolladores API en "API Permissions" (no leída). Google Calendar, WhatsApp, Mercado Pago, ARCA: No evidenciado.

## 9. Operación
Multi-ubicación con tarifa por volumen — Comprobado (https://www.opendental.com/site/fees.html). Conversión desde "200+ PMS" — Afirmación comercial (https://www.opendental.com/). Capacitación con trainer pago — Comprobado (https://www.opendental.com/site/order.html). Foro de soporte — https://www.opendental.com/ (listado). Roles/auditoría/exportación: No evidenciado en páginas leídas.

## 10. Seguridad y cumplimiento
Menciona BAA para desarrolladores de API (HIPAA) — Comprobado (https://opendental.com/site/apispecification.html). Cifrado, backups, trazabilidad, normativa argentina: No evidenciado.

## 11. Modelo comercial
Precios en USD (Comprobado, https://www.opendental.com/site/fees.html): Soporte mensual por ubicación — EE.UU. $199 (12 meses, luego $149); Canadá $164 (luego $137); "Other Countries" $89; "Developing Countries" Free con soporte limitado; más de 3 proveedores $20/mes/proveedor. eServices: Bundle $165/mes/ubicación; Web Sched Recall $75; Web Sched New+Existing Patient $75; Web Sched ASAP $20 (gratis con Recall); eClipboard $45; eConfirmations $25 ($40 con landline); Secure Email $30; ODMobile $35; AI Image Analysis $149. Texting: número asignado $5/mes (EE.UU./PR/Canadá); por mensaje saliente $0.04 EE.UU.; tarifas internacionales en otra página (no leída). Contrato 12 meses inicial en EE.UU. (https://www.opendental.com/site/order.html). Prueba: disponible (https://www.opendental.com/).

## 12. Fortalezas, limitaciones y diferenciales evidentes
(Opinión del analista.) Fortalezas: precios y paquetes totalmente transparentes; API REST documentada; Web Sched con ASAP; tarifa reducida/gratis para otros países. Limitaciones: no es open source actualmente; producto de escritorio centrado en EE.UU.; sin localización argentina evidenciada; interfaz en inglés. Diferencial: modelo de precio por ubicación con eServices modulares y lista ASAP con enlaces de aceptación por paciente (referencia de UX).

## 13. Evidencia de adopción
Capterra: 4,6 (87 reseñas) según resultado de búsqueda de Capterra (https://www.capterra.com/compare/2329-122350/Dentrix-vs-Open-Dental; no leído directamente; las cifras cambian). Cantidad de clientes: No evidenciado.

## Puntaje preliminar 0-5 (para la matriz B)
| Criterio | Peso | Nota 0-5 | Justificación breve |
|---|---|---|---|
| Gestión de turnos y automatización | 25% | 3.5 | Web Sched (3 modos) + ASAP comprobados; falta lista de espera clásica, WhatsApp |
| Funcionalidad odontológica clínica | 20% | 3.5 | Odontograma, recetas, formularios, IA de imágenes; perio/presupuestos no evidenciados |
| Integraciones locales y WhatsApp | 15% | 3.5 | API REST documentada y puentes; sin WhatsApp ni local AR |
| Administración, cobros y facturación | 15% | 3 | Portal de pagos; facturación AR ausente |
| Experiencia del paciente | 10% | 3.5 | Reserva online, portal, eClipboard |
| Seguridad, exportación y trazabilidad | 10% | 2.5 | Solo BAA/API; resto no evidenciado |
| Precio y facilidad de adopción | 5% | 4.5 | Tarifas publicadas completas; tarifa reducida para otros países |
**Total ponderado preliminar: 3.4**
Regla: un ítem "No evidenciado" puntúa como ausente (no suma). Afirmación comercial suma la mitad que Comprobado.

## Fuentes consultadas
- https://www.opendental.com/ — sitio oficial — consultada 2026-11-20
- https://www.opendental.com/site/fees.html — precios — consultada 2026-11-20
- https://www.opendental.com/site/order.html — precios/contrato — consultada 2026-11-20
- https://www.opendental.com/site/websched.html — documentación de servicio — consultada 2026-11-20
- https://www.opendental.com/site/eservicesbundle.html — sitio oficial — consultada 2026-11-20
- https://opendental.com/site/apispecification.html — documentación pública (API) — consultada 2026-11-20
- https://opendental.com/site/license.html — cambio de licencia GPL a propietaria (vía resultado de búsqueda; no leída directamente) — 2026-11-20
