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
        String consentimientoVersion,
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
        return new Paciente(null, nombre, apellido, dni, email, telefono, obraSocial, nroAfiliado, notas, true, null, null, null);
    }

    /**
     * Ficha creada por la reserva online (US-015, RN-26): sin DNI y con el consentimiento aceptado en
     * {@code aceptadoEn} con la versión del texto (PA-08). El alta y el consentimiento comparten el instante.
     */
    public static Paciente nuevoDeReservaOnline(String nombre, String apellido, String email, String telefono,
            String obraSocial, Instant aceptadoEn, String versionConsentimiento) {
        return new Paciente(null, nombre, apellido, null, email, telefono, obraSocial, null, null, true, aceptadoEn,
                versionConsentimiento, aceptadoEn);
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
                this.consentimientoVersion,
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
                this.consentimientoVersion,
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
                this.consentimientoVersion,
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
