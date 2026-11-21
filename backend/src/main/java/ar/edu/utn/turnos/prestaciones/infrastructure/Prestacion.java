package ar.edu.utn.turnos.prestaciones.infrastructure;

import ar.edu.utn.turnos.prestaciones.domain.DuracionPrestacion;
import ar.edu.utn.turnos.prestaciones.domain.NombrePrestacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Check;

/**
 * Prestación del catálogo. {@code nombre_clave} materializa la unicidad sin distinguir mayúsculas
 * (Hibernate genera su UNIQUE) y el CHECK repite RN-09 en la base como defensa en profundidad.
 */
@Entity
@Table(name = "prestacion")
@Check(name = "prestacion_duracion_rn09",
        constraints = "duracion_default_min BETWEEN 5 AND 480 AND duracion_default_min % 5 = 0")
class Prestacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "nombre_clave", nullable = false, unique = true, length = 100)
    private String nombreClave;

    @Column(name = "duracion_default_min", nullable = false)
    private int duracionDefaultMin;

    @Column(nullable = false)
    private boolean activa;

    protected Prestacion() {
    }

    Prestacion(NombrePrestacion nombre, DuracionPrestacion duracion) {
        asignar(nombre, duracion);
        this.activa = true;
    }

    void modificar(NombrePrestacion nombre, DuracionPrestacion duracion, boolean activa) {
        asignar(nombre, duracion);
        this.activa = activa;
    }

    void desactivar() {
        this.activa = false;
    }

    private void asignar(NombrePrestacion nombre, DuracionPrestacion duracion) {
        this.nombre = nombre.valor();
        this.nombreClave = nombre.clave();
        this.duracionDefaultMin = duracion.minutos();
    }

    Long getId() {
        return id;
    }

    String getNombre() {
        return nombre;
    }

    String getNombreClave() {
        return nombreClave;
    }

    int getDuracionDefaultMin() {
        return duracionDefaultMin;
    }

    boolean isActiva() {
        return activa;
    }
}
