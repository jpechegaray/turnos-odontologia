package ar.edu.utn.turnos.seguridad.infrastructure;

import ar.edu.utn.turnos.seguridad.domain.EmailUsuario;
import ar.edu.utn.turnos.seguridad.domain.PoliticaBloqueo;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "usuario", uniqueConstraints = @UniqueConstraint(
        name = Usuario.RESTRICCION_PROFESIONAL, columnNames = "profesional_id"))
class Usuario {

    /** Un profesional tiene a lo sumo un usuario vinculado (C-13). */
    static final String RESTRICCION_PROFESIONAL = "uk_usuario_profesional";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, length = 60)
    private String passwordHash;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo;

    @Column(nullable = false)
    private int intentosFallidos;

    private Instant bloqueadoHasta;

    @Column(nullable = false)
    private Instant creadoEn;

    /** Id del profesional del módulo profesionales, sin FK entre módulos (KB 08); solo para ODONTOLOGO. */
    @Column(name = "profesional_id")
    private Long profesionalId;

    protected Usuario() {
    }

    Usuario(String email, String passwordHash, String nombre, Rol rol, Instant creadoEn) {
        this(email, passwordHash, nombre, rol, null, creadoEn);
    }

    Usuario(String email, String passwordHash, String nombre, Rol rol, Long profesionalId, Instant creadoEn) {
        if (!passwordHash.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}")) {
            throw new IllegalArgumentException("Se requiere un hash BCrypt");
        }
        this.email = EmailUsuario.normalizar(email);
        this.passwordHash = passwordHash;
        this.nombre = nombre;
        this.rol = rol;
        this.profesionalId = profesionalId;
        this.activo = true;
        this.creadoEn = creadoEn;
    }

    /** Las reglas (vínculo según rol, último ADMIN, unicidad) las valida GestionUsuarios antes de llamar. */
    void modificar(String email, String nombre, Rol rol, Long profesionalId) {
        this.email = EmailUsuario.normalizar(email);
        this.nombre = nombre;
        this.rol = rol;
        this.profesionalId = profesionalId;
    }

    void desactivar() {
        activo = false;
    }

    void reactivar() {
        activo = true;
    }

    String getNombre() {
        return nombre;
    }

    Long getProfesionalId() {
        return profesionalId;
    }

    String getEmail() {
        return email;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    Long getId() {
        return id;
    }

    Rol getRol() {
        return rol;
    }

    boolean isActivo() {
        return activo;
    }

    PoliticaBloqueo.Estado estadoDeBloqueo() {
        return new PoliticaBloqueo.Estado(intentosFallidos, bloqueadoHasta);
    }

    void actualizarBloqueo(PoliticaBloqueo.Estado estado) {
        intentosFallidos = estado.intentosFallidos();
        bloqueadoHasta = estado.bloqueadoHasta();
    }
}
