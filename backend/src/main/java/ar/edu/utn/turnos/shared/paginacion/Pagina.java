package ar.edu.utn.turnos.shared.paginacion;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.function.Function;

/**
 * Una página de resultados, con el formato que usa la API para todo listado paginado (C-25, D1):
 * {@code contenido}, {@code pagina} (desde 0), {@code tamanio}, {@code totalElementos} y
 * {@code totalPaginas}. No depende de Spring Data, así que también la pueden devolver los puertos de dominio.
 */
public record Pagina<T>(List<T> contenido, int pagina, int tamanio, long totalElementos) {

    public Pagina {
        if (pagina < 0) {
            throw new IllegalArgumentException("El número de página no puede ser negativo");
        }
        if (tamanio < 1) {
            throw new IllegalArgumentException("El tamaño de página debe ser al menos 1");
        }
        if (totalElementos < 0) {
            throw new IllegalArgumentException("El total de elementos no puede ser negativo");
        }
        contenido = List.copyOf(contenido);
    }

    /** Cantidad de páginas para el total y el tamaño: 0 si no hay elementos. */
    @JsonProperty("totalPaginas")
    public long totalPaginas() {
        return (totalElementos + tamanio - 1) / tamanio;
    }

    /** Transforma el contenido (por ejemplo, de dominio a DTO) y conserva página, tamaño y total. */
    public <R> Pagina<R> map(Function<? super T, ? extends R> transformacion) {
        List<R> transformado = contenido.stream().<R>map(transformacion).toList();
        return new Pagina<>(transformado, pagina, tamanio, totalElementos);
    }
}
