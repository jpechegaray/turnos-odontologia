package ar.edu.utn.turnos.pacientes.domain;

import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.time.Instant;

public record Paciente(
        Long id,
        String nombre,
        String apellido,
        String dni,
        String email,
        String telefono,
        String obraSocial,
        String nroAfiliado,
        String notas,
        boolean activo,
        Instant consentimientoDatosEn,
        Instant creadoEn) {

    public Paciente {
        nombre = normalizar(nombre);
        apellido = normalizar(apellido);
        dni = normalizar(dni);
        email = normalizar(email);
        telefono = normalizar(telefono);
        obraSocial = normalizar(obraSocial);
        nroAfiliado = normalizar(nroAfiliado);
        notas = normalizar(notas);

        if (nombre == null || nombre.isEmpty()) {
            throw new ReglaNegocioException("RN-32", EstadoError.NO_PROCESABLE_422, "El nombre es obligatorio");
        }
        if (apellido == null || apellido.isEmpty()) {
            throw new ReglaNegocioException("RN-32", EstadoError.NO_PROCESABLE_422, "El apellido es obligatorio");
        }
        if (email == null && telefono == null) {
            throw new ReglaNegocioException("RN-32", EstadoError.NO_PROCESABLE_422, "El paciente debe tener al menos un medio de contacto");
        }
    }

    public static Paciente nuevo(
            String nombre,
            String apellido,
            String dni,
            String email,
            String telefono,
            String obraSocial,
            String nroAfiliado,
            String notas) {
        return new Paciente(null, nombre, apellido, dni, email, telefono, obraSocial, nroAfiliado, notas, true, null, null);
    }

    public Paciente conCreacion(Instant creadoEn) {
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
                creadoEn);
    }

    public Paciente conDatos(
            String nombre,
            String apellido,
            String dni,
            String email,
            String telefono,
            String obraSocial,
            String nroAfiliado,
            String notas) {
        return new Paciente(
                this.id,
                nombre,
                apellido,
                dni,
                email,
                telefono,
                obraSocial,
                nroAfiliado,
                notas,
                this.activo,
                this.consentimientoDatosEn,
                this.creadoEn);
    }

    public Paciente dadoDeBaja() {
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
                false,
                this.consentimientoDatosEn,
                this.creadoEn);
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}
