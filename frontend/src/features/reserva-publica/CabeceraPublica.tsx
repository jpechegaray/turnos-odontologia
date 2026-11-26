import { Logo, NombreMarca } from "../../shared/ui";

/** Franja superior de las pantallas para pacientes: la marca del sistema, sin navegación del personal. */
export function CabeceraPublica() {
  return (
    <div className="cabecera-publica">
      <div className="cabecera-publica__contenido">
        <span className="cabecera-publica__logo"><Logo /></span>
        <span className="cabecera-publica__marca">
          <strong aria-label={window.BRAND.name}><NombreMarca /></strong>
          <small>Turnos online</small>
        </span>
      </div>
    </div>
  );
}

/** Barras de progreso de la reserva: la actual y las anteriores quedan marcadas. */
export function PasosReserva({ pasos, actual }: Readonly<{ pasos: readonly string[]; actual: number }>) {
  return (
    <ol className="pasos" aria-label="Pasos de la reserva">
      {pasos.map((paso, i) => (
        <li key={paso} data-estado={i < actual ? "hecho" : undefined} aria-current={i === actual ? "step" : undefined}>
          {`${i + 1} · ${paso}`}
        </li>
      ))}
    </ol>
  );
}
