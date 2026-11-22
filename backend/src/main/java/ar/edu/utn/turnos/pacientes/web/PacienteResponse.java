package ar.edu.utn.turnos.pacientes.web;

import java.time.Instant;
import java.util.Objects;

/**
 * DTO de salida para exponer datos de la ficha de paciente.
 */
public class PacienteResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String dni;
    private String email;
    private String telefono;
    private String obraSocial;
    private boolean activo;
    private Instant creadoEn;
    private Instant actualizadoEn;

    // Getters y setters (Jackson)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getObraSocial() { return obraSocial; }
    public void setObraSocial(String obraSocial) { this.obraSocial = obraSocial; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Instant actualizadoEn) { this.actualizadoEn = actualizadoEn; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PacienteResponse)) return false;
        PacienteResponse that = (PacienteResponse) o;
        return activo == that.activo &&
                Objects.equals(id, that.id) &&
                Objects.equals(nombre, that.nombre) &&
                Objects.equals(apellido, that.apellido) &&
                Objects.equals(dni, that.dni) &&
                Objects.equals(email, that.email) &&
                Objects.equals(telefono, that.telefono) &&
                Objects.equals(obraSocial, that.obraSocial) &&
                Objects.equals(creadoEn, that.creadoEn) &&
                Objects.equals(actualizadoEn, that.actualizadoEn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nombre, apellido, dni, email, telefono, obraSocial, activo, creadoEn, actualizadoEn);
    }
}
