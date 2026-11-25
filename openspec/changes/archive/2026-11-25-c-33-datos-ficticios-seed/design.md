# Design

## Context

El perfil dev ya inicializa un administrador si se configura SEED_ADMIN_PASSWORD. C-33 conserva ese comportamiento y añade un ApplicationRunner en shared.seed para preparar una agenda de demostración (US-021).

## Goals / Non-Goals

- Crear el consultorio de la base de conocimiento usando las reglas de los servicios existentes.
- Mantener la carga optativa, atómica e idempotente en desarrollo.
- La carga se aplica una sola vez por base vacía. La demostración histórica permanece disponible en la fecha que se sembró, sin generar nuevos turnos en cada arranque.

## Decisions

- Un bloqueo transaccional de PostgreSQL serializa las cargas del seed. La comprobación de base vacía incluye catálogos, pacientes, turnos y usuarios salvo el administrador de desarrollo. Se admite la configuración inicial que ya crea el sistema.
- La clave de personal se toma de SEED_STAFF_PASSWORD y se valida con la política existente. GestionUsuarios guarda los hashes BCrypt y valida los vínculos. El seed nunca registra contraseñas.
- Se reutilizan las altas de catálogo, pacientes y CrearTurnoService. El runner dev instala un contexto interno ADMIN solo durante la carga y restaura el contexto previo en un finally. No se agregan permisos web.
- Se guardan dos turnos contiguos con Ana a las 10:00 y 10:30, y un control con Bruno, para el primer día hábil con inicio a las 10:00 aún futuro en Buenos Aires. Así el ejemplo es válido incluso si se ejecuta en fin de semana.
- Si ya hay datos operativos, se registra el motivo y la transacción termina sin escrituras. Si falla un alta, la transacción revierte la carga completa.

## Risks / Trade-offs

El seed reutiliza las reglas de negocio para evitar desvíos entre la demostración y la operatoria habitual. La comprobación de base vacía usa consultas SQL de existencia porque debe detectar también los registros dados de baja.

## Migration Plan

Sin migración. En una base dev vacía, configurar SEED_ADMIN_PASSWORD y SEED_STAFF_PASSWORD y arrancar la aplicación.

## Open Questions

Ninguna.
