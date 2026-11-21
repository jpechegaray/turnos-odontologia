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
import java.time.Instant;

@Entity
@Table(name = "usuario")
class Usuario {

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

    protected Usuario() {
    }

    Usuario(String email, String passwordHash, String nombre, Rol rol, Instant creadoEn) {
        if (!passwordHash.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}")) {
            throw new IllegalArgumentException("Se requiere un hash BCrypt");
        }
        this.email = EmailUsuario.normalizar(email);
        this.passwordHash = passwordHash;
        this.nombre = nombre;
        this.rol = rol;
        this.activo = true;
        this.creadoEn = creadoEn;
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
