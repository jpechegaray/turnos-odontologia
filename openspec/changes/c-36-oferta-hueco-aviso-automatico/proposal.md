# Proposal

## Why

C-27 ya publica `HuecoLiberado` al cancelar un turno y C-31 ya sabe qué entradas de la lista de espera son compatibles con un hueco y a cuáles avisar (RN-22, RN-23, SU-14). Falta la pieza que une las dos: avisar solo, por email y con un enlace seguro, a los pacientes compatibles (RN-06, RN-23, RN-38; DD-10, el diferenciador del producto). Es el siguiente eslabón del camino crítico (C-27 → C-36 → C-42) y desbloquea C-42 (tomar el hueco desde la oferta) y C-43 (asignación manual). Todas sus dependencias (C-08, C-14, C-15, C-18, C-27, C-31) están en `master`.

## What Changes

- **Entidad `OfertaHueco`** en `listaespera.oferta`: `lista_espera_id`, `profesional_id`, `hueco_inicio`, `hueco_fin`, `sillon_sugerido_id`, `token_hash` (SHA-256, único), `expira_en` (= `hueco_inicio`), `estado` ∈ {`ENVIADA`, `TOMADA`, `VENCIDA`} y `creado_en`. A lo sumo una oferta por `(lista_espera_id, profesional_id, hueco_inicio)`; `CHECK (hueco_fin > hueco_inicio)`. El token en claro nunca se persiste ni se loguea.
- **Aviso automático al liberarse un hueco:** un oyente de `HuecoLiberado` que corre **después del commit** de la cancelación (si la cancelación se revierte, no se avisa). Usa `BuscadorCompatibles.aAvisar` (compatibilidad + tope por antigüedad + un aviso por paciente + nada si el hueco ya empezó), crea una oferta por entrada elegida y envía un email `OFERTA_HUECO` por paciente. Una sola tanda por hueco: si el mismo hueco se vuelve a liberar, no se duplican ofertas ni emails.
- **Email `OFERTA_HUECO`** por `ServicioNotificaciones` (C-15) con el enlace `APP_PUBLIC_URL/oferta/{token}`, la fecha y hora del hueco, la prestación y el profesional. Una entrada cuyo paciente no tiene email no genera oferta.
- **Reintentos del aviso:** como el cuerpo no se guarda, el reintento de un aviso fallido **rota el token** de la oferta (el enlace anterior nunca llegó) y reenvía el enlace nuevo. Los reintentos cesan al empezar el hueco (ya lo garantiza C-15) o si la oferta dejó de estar `ENVIADA`.
- **Planificador de vencimiento:** las ofertas `ENVIADA` cuyo `expira_en` ya pasó pasan a `VENCIDA`.
- **Consulta interna de oferta por token** (`OfertasHueco.buscarVigentePorToken`) que compara por hash y no distingue entre token inexistente, vencido o tomado; la usará C-42. Sin endpoint HTTP en este change.
- **Configuración:** `app.listaespera.url-publica` (de `APP_PUBLIC_URL`) y el intervalo del planificador de vencimiento, apagado en el perfil `test`.
- **Quedan fuera:**
  - la página pública `/oferta/{token}` y tomar el hueco (C-42);
  - la asignación manual por recepción (C-43);
  - el hueco liberado por reprogramación (C-35 publica el evento; el extremo a extremo es C-51);
  - el frontend de lista de espera (C-47).

## Capabilities

### New Capabilities

- `oferta-hueco`: ofertas de hueco a la lista de espera: creación automática al liberarse un hueco, token seguro, email de aviso con reintento, vigencia y vencimiento, y búsqueda por token.

### Modified Capabilities

Ninguna. `lista-espera` (compatibilidad y selección) y `notificaciones-email` (registro, idempotencia y reintentos) se consumen sin cambiar sus requisitos.

## Impact

- **Nuevo paquete `listaespera.oferta`** (domain, application, infrastructure): `OfertaHueco`, `EstadoOferta`, repositorio JPA, `AvisoHuecoLiberado` (oyente), `OfertasHueco` (servicio), `ReconstructorAvisoOferta` (implementa `ReconstructorDatosNotificacion`) y `PlanificadorVencimientoOfertas`.
- **Consume sin cambios:** `HuecoLiberado` (agenda), `BuscadorCompatibles`, `ServicioToken`, `ServicioNotificaciones`, `PacienteConsulta`, `ConsultaPrestaciones`, `ProfesionalConsulta` y `ConfiguracionServicio.maxAvisosPorHueco()`.
- **Sin cambios en C-15:** para no mandar un email sin enlace cuando la oferta ya no está `ENVIADA`, el reconstructor de C-36 usa el gancho `descartarSiFaltanDatos` que agregó C-37 en `ReconstructorDatosNotificacion`. Un primer borrador cambiaba `ServicioNotificaciones`; se descartó al integrar con master.
- **Esquema:** tabla `oferta_hueco` generada por Hibernate (unique y check incluidos); si Hibernate no genera el `CHECK` o el índice `(estado, expira_en)`, van al script idempotente `schema-constraints.sql`.
- **Configuración:** `application.yml` (bloque `listaespera`) y `application-test.yml`.
- **Sin cambios de API HTTP ni de seguridad.**
