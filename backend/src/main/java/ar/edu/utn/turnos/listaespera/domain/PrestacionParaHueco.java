package ar.edu.utn.turnos.listaespera.domain;

/** Lo que la compatibilidad necesita de la prestación de una entrada: su duración por defecto y si está activa. */
public record PrestacionParaHueco(int duracionMin, boolean activa) {
}
