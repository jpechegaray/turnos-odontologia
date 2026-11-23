package ar.edu.utn.turnos.pacientes.infrastructure;

import ar.edu.utn.turnos.pacientes.domain.Paciente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * JPA entity for {@link Paciente}. Maps the domain record to a persistent table.
 * The table name and constraints follow the data model (KB 04).
 */
@Entity
@Table(name = "pacientes",
    uniqueConstraints = @UniqueConstraint(name = "uk_paciente_dni", columnNames = "dni"))
public class PacienteJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    // DNI is nullable but must be unique when present
    @Column(nullable = true, unique = true)
    private String dni;

    @Column(nullable = true)
    private String email;

    @Column(nullable = true)
    private String telefono;

    @Column(nullable = true)
    private String obraSocial;

    @Column(nullable = true)
    private String nroAfiliado;

    @Column(columnDefinition = "text", nullable = true)
    private String notas;

    @Column(nullable = false)
    private boolean activo;

    @Column(nullable = true)
    private Instant consentimientoDatosEn;

    @Column(nullable = false, updatable = false)
    private Instant creadoEn;

    // ---------- Constructors ----------
    protected PacienteJpa() {
        // JPA default constructor
    }

    private PacienteJpa(Paciente dominio) {
        this.id = dominio.id();
        this.nombre = dominio.nombre();
        this.apellido = dominio.apellido();
        this.dni = dominio.dni();
        this.email = dominio.email();
        this.telefono = dominio.telefono();
        this.obraSocial = dominio.obraSocial();
        this.nroAfiliado = dominio.nroAfiliado();
        this.notas = dominio.notas();
        this.activo = dominio.activo();
        this.consentimientoDatosEn = dominio.consentimientoDatosEn();
        this.creadoEn = dominio.creadoEn();
    }

    public static PacienteJpa desdeDominio(Paciente dominio) {
        return new PacienteJpa(dominio);
    }

    public Paciente aDominio() {
        return new Paciente(
                this.id,
                this.nombre,
                this.apellido,
                this.dni,
                this.email,
                this.telefono,
                this.obraSocial,
                this.nroAfiliado,
                this.notas,
                this.activo,
                this.consentimientoDatosEn,
                this.creadoEn);
    }
}
