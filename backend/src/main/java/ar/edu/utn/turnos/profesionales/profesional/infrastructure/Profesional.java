package ar.edu.utn.turnos.profesionales.profesional.infrastructure;

import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta.Datos;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "profesional", uniqueConstraints = @UniqueConstraint(
        name = Profesional.RESTRICCION_MATRICULA, columnNames = "matricula"))
class Profesional {

    static final String RESTRICCION_MATRICULA = "uk_profesional_matricula";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false)
    private String matricula;

    private String especialidad;

    @Column(nullable = false)
    private boolean activo;

    protected Profesional() {
    }

    Profesional(String nombre, String apellido, String matricula, String especialidad, boolean activo) {
        actualizar(nombre, apellido, matricula, especialidad);
        this.activo = activo;
    }

    void actualizar(String nombre, String apellido, String matricula, String especialidad) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.matricula = matricula.strip();
        this.especialidad = especialidad;
    }

    Long getId() {
        return id;
    }

    String getMatricula() {
        return matricula;
    }

    String getEspecialidad() {
        return especialidad;
    }

    boolean isActivo() {
        return activo;
    }

    void desactivar() {
        activo = false;
    }

    void reactivar() {
        activo = true;
    }

    Datos datos() {
        return new Datos(id, nombre, apellido, matricula, especialidad, activo);
    }
}
