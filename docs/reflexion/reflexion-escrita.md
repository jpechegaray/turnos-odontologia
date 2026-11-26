# Reflexión escrita: Turnos Odontología

**Materia:** Desarrollo de Software, Trabajo Global (UTN)
**Integrantes:** Juan Pablo Echegaray (líder), Santino Escoriaza, Valentino Friscolanti, Lionel López, Julián Salinas, Nicolás Day
**Fecha:** 26/11/2026

## Uso de la IA (declaración)

Este proyecto se elaboró con el apoyo de varios modelos de inteligencia artificial: Claude, OpenCode, Codex y Gemini, junto con OpenSpec y Active Stack. Los agentes generaron la mayor parte del Discovery, la base de conocimiento, el roadmap y el código de cada change. Nosotros conducimos cada etapa, revisamos lo generado con el protocolo de inspección y aprobamos cada PR antes de mergearlo a master.

## 1. Información del Discovery incorrecta, no verificable o inventada

El informe analizó 23 sistemas, pero casi toda la información de cada uno salía de las páginas de venta de los proveedores y casi no había funciones comprobadas. Por ejemplo, el informe ubicaba la Ley 25.326 de DentalCore en su página principal, pero al revisar la fuente figura en la política de privacidad.

También ocurrió que encontrábamos contradicciones con los precios, por ejemplo para DentalCore. Al abrirlo nosotros mismos vimos que los precios cambian según el país desde donde se consultaba.

Lo detectamos al darnos cuenta de las inconsistencias en algunos precios y comparando lo que nos resumió el agente en el informe con lo que veíamos en la URL citada.

## 2. Dónde usamos Ajustar y qué habría pasado con Continuar

Al momento de hacer el roadmap, el primer CHANGES.md que generó el agente le asignaba a Nicolás todas las tareas de la ola 0, lo que causaba un cuello de botella para el resto del equipo ya que no podíamos avanzar hasta que Nicolás no terminara todos sus changes antes. Para solucionar este problema usamos Ajustar: pasamos C-01 y C-02 a Santino y la seguridad (C-03) a Nicolás. Si hubiéramos apretado Continuar, el proyecto habría arrancado con una sola persona trabajando y el resto esperando.

## 3. Especificación aprobada vs. prompt suelto

Notamos muchas ventajas a la hora de usar especificaciones por sobre prompts sueltos, una de las principales fue la documentación de decisiones e implementaciones del sistema, debido a esto se podía trabajar en varias sesiones sin tener que contextualizar manualmente al modelo cada vez. También limitaba al agente a la hora de “inventar”, fijándole un alcance fijo sabiendo que lo que quedaba fuera de un change, iba a estar dentro de otro. Otra ventaja a destacar fue la practicidad para trabajar en equipo, con especificaciones los seis teníamos distintos agentes trabajando con las mismas reglas y changes que encajaban entre sí.

## 4. Qué parte de la fundación aportó más valor y cuál menos

Consideramos que lo que más valor aportó en la fundación fueron las reglas de negocio y las reglas de los agentes. Cada proposal de un change citaba a una RN y las reglas de los agentes evitaban problemas como que un modelo use JWT en lugar de sesión, o use H2 en los tests.

Respecto a algo que haya aportado menos valor creemos que el archivo de CHANGES.md no fue tan útil a la hora de consultar las dependencias, en la práctica usábamos mucho más el tablero de github, sin embargo CHANGES.md seguía siendo clave ya que era el que definía los changes.

## 5. Qué hizo cada integrante y en qué etapa

| Integrante | Etapa / rol | Changes principales (según commits) |
|---|---|---|
| **Juan Pablo** (líder) | Fundación completa, repositorio de GitHub con master protegida, ramas e issues, CI de tests. Revisó y mergeó los PRs y mantuvo el roadmap al día. | C-19, C-27, C-35, C-37 |
| **Santino** | Esqueleto inicial (Ola 0) y copia de skills para otros agentes | C-01, C-02, C-14, C-23 |
| **Valentino** | Catálogos, agenda y lista de espera, back y front | C-13, C-18, C-28, C-36 |
| **Lionel** | Reserva online pública y propuesta PA-08 (consentimiento y datos personales) | C-17, C-38, C-41, C-43 |
| **Julián** | Pacientes, notificaciones por email y front de configuración | C-07, C-15, C-20, C-40 |
| **Nicolás** | Seguridad y acceso del personal, tokens del turno | C-03, C-12, C-16, C-32 |
