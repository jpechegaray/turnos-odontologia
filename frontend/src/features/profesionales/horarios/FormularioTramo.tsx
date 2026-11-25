import { useId, useState, type FormEvent } from "react";
import { Alerta, Boton, Campo } from "../../../shared/ui";
import { DIAS_SEMANA } from "./dias";
import type { TramoHorario, TramoSolicitud } from "./tipos";
import { validarTramo } from "./validacion";

export type FormularioTramoProps = Readonly<{
  /** Con un tramo, el formulario lo modifica; sin él, agrega uno nuevo. */
  valorInicial?: TramoHorario;
  /** Tramos actuales del profesional, para validar la superposición antes de enviar (RN-16). */
  existentes: readonly TramoHorario[];
  /** Si rechaza, el formulario conserva lo ingresado (el panel muestra el error). */
  onEnviar: (solicitud: TramoSolicitud) => Promise<void>;
  onCancelar?: () => void;
  deshabilitado: boolean;
}>;

/** Día de la semana y horas `HH:mm`; valida localmente y no llama a la API si el tramo es inválido. */
export function FormularioTramo({ valorInicial, existentes, onEnviar, onCancelar, deshabilitado }: FormularioTramoProps) {
  const idDia = useId();
  const [diaSemana, actualizarDia] = useState(String(valorInicial?.diaSemana ?? 1));
  const [horaInicio, actualizarInicio] = useState(valorInicial?.horaInicio ?? "");
  const [horaFin, actualizarFin] = useState(valorInicial?.horaFin ?? "");
  const [error, actualizarError] = useState<string>();
  const editando = valorInicial !== undefined;

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    const solicitud: TramoSolicitud = { diaSemana: Number(diaSemana), horaInicio, horaFin };
    const motivo = validarTramo(solicitud, existentes, valorInicial?.id);
    actualizarError(motivo);
    if (motivo) return;
    // El panel informa el error; acá solo se evita el rechazo sin manejar.
    onEnviar(solicitud).catch(() => undefined);
  }

  return (
    <form
      className="agenda-formulario"
      aria-label={editando ? "Modificar tramo" : "Nuevo tramo"}
      noValidate
      onSubmit={enviar}
    >
      <div className="field">
        <label htmlFor={idDia}>Día</label>
        <select id={idDia} className="select" value={diaSemana} onChange={(e) => actualizarDia(e.target.value)}>
          {DIAS_SEMANA.map(({ dia, nombre }) => (
            <option key={dia} value={dia}>{nombre}</option>
          ))}
        </select>
      </div>
      <Campo etiqueta="Desde" type="time" value={horaInicio} onChange={(e) => actualizarInicio(e.target.value)} />
      <Campo etiqueta="Hasta" type="time" value={horaFin} onChange={(e) => actualizarFin(e.target.value)} />
      {error && <Alerta tipo="peligro" titulo={error} />}
      <div className="agenda-formulario__acciones">
        <Boton type="submit" disabled={deshabilitado}>{editando ? "Guardar cambios" : "Agregar tramo"}</Boton>
        {onCancelar && (
          <Boton variante="secundaria" disabled={deshabilitado} onClick={onCancelar}>Cancelar</Boton>
        )}
      </div>
    </form>
  );
}
