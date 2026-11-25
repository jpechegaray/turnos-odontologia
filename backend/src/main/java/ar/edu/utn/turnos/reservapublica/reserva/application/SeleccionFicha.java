package ar.edu.utn.turnos.reservapublica.reserva.application;

import ar.edu.utn.turnos.pacientes.domain.Paciente;
import java.util.List;
import java.util.Optional;

/**
 * Deduplicación de la reserva online (RN-28, SU-04). Recibe las fichas activas del email ordenadas de la más
 * antigua a la más nueva: con una sola la reutiliza; con varias, la que coincide en los dígitos del teléfono
 * y, si ninguna coincide, la más antigua. Un teléfono nulo o sin dígitos nunca coincide, ni con otro vacío.
 */
public final class SeleccionFicha {

    private SeleccionFicha() {
    }

    public static Optional<Paciente> elegir(List<Paciente> fichasDelEmail, String telefono) {
        if (fichasDelEmail.isEmpty()) {
            return Optional.empty();
        }
        var buscado = digitos(telefono);
        if (fichasDelEmail.size() > 1 && !buscado.isEmpty()) {
            for (var ficha : fichasDelEmail) {
                if (buscado.equals(digitos(ficha.telefono()))) {
                    return Optional.of(ficha);
                }
            }
        }
        return Optional.of(fichasDelEmail.get(0));
    }

    private static String digitos(String telefono) {
        return telefono == null ? "" : telefono.replaceAll("\\D", "");
    }
}
