package ar.edu.utn.turnos.pacientes.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

/**
 * DTO de entrada para crear o actualizar una ficha de paciente.
 *
 * Según la especificación, al menos uno de {@code email} o {@code telefono}
 * debe estar presente; la validación de esa regla está delegada al
 * {@link ar.edu.utn.turnos.pacientes.application.PacienteServicio} que lanza
 * {@code ReglaNegocioException} con código {@code RN-32} cuando se viola.
 */
public class PacienteRequest {

    @NotBlank
    @Size(max = 50)
    private String nombre;

    @NotBlank
    @Size(max = 50)
    private String apellido;

    @Nullable
    @Size(max = 20)
    private String dni;

    @Nullable
    @Size(max = 100)
    private String email;

    @Nullable
    @Size(max = 20)
    private String telefono;

    @Nullable
    @Size(max = 200)
    private String obraSocial;

    // Getters y setters (para Jackson)
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
}
