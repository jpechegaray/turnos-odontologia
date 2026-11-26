# Diseño

El servicio central reutiliza el saludo, la fecha en Buenos Aires, el profesional, la prestación y la firma. Cada tipo define asunto y mensaje principal. El alta en lista de espera muestra la prestación y las condiciones del aviso por correo.

El alta del personal y el ingreso a la lista publican eventos con identificadores. Se notifican después del commit. Al agendar desde la lista, se usa un tipo propio tanto para la asignación manual como para la aceptación de la oferta. La confirmación de reserva pública directa continúa con su tipo actual. La confirmación de lista se deduplica por turno, tipo e inicio. El alta de espera se deduplica por entrada.

La vista previa toma los datos asociados al aviso y usa el mismo formateador. La aplicación genera el enlace seguro al enviar una reserva en línea o una oferta. La vista previa omite esos enlaces. El registro conserva referencias y el resultado del envío.
