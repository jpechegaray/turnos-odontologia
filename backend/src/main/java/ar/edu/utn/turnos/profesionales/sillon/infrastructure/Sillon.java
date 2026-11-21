package ar.edu.utn.turnos.profesionales.sillon.infrastructure;

import ar.edu.utn.turnos.profesionales.sillon.application.SillonConsulta.Datos;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "sillon", uniqueConstraints = @UniqueConstraint(name = Sillon.RESTRICCION_NOMBRE, columnNames = "nombre"))
class Sillon {

    static final String RESTRICCION_NOMBRE = "uk_sillon_nombre";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(nullable = false)
    private boolean activo;

    protected Sillon() {
    }

    Sillon(String nombre, String descripcion, boolean activo) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activo = activo;
    }

    Long getId() {
        return id;
    }

    Datos datos() {
        return new Datos(id, nombre, descripcion, activo);
    }

    void editar(String nombre, String descripcion, boolean activo) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activo = activo;
    }
}
