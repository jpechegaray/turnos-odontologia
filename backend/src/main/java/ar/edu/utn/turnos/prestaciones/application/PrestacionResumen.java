package ar.edu.utn.turnos.prestaciones.application;

/** Vista de solo lectura de una prestación para la API y para otros módulos. */
public record PrestacionResumen(Long id, String nombre, int duracionDefaultMin, boolean activa) {
}
