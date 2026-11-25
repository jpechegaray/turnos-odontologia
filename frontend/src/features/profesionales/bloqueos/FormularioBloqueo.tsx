import { useState, type FormEvent } from "react";
import { Boton, Campo } from "../../../shared/ui";
import type { BloqueoSolicitud, FormularioBloqueoValores } from "./tipos";
import { validarBloqueo, type ErroresBloqueo } from "./validacion";

const VACIO: FormularioBloqueoValores = { fechaInicio: "", horaInicio: "", fechaFin: "", horaFin: "", motivo: "" };

export type FormularioBloqueoProps = Readonly<{
  /** Si rechaza, el formulario conserva lo ingresado (el panel muestra el error). */
  onEnviar: (solicitud: BloqueoSolicitud) => Promise<void>;
  deshabilitado: boolean;
}>;

/** Inicio y fin en hora de Buenos Aires y motivo; valida localmente y no llama a la API si es inválido. */
export function FormularioBloqueo({ onEnviar, deshabilitado }: FormularioBloqueoProps) {
  const [valores, actualizarValores] = useState<FormularioBloqueoValores>(VACIO);
  const [errores, actualizarErrores] = useState<ErroresBloqueo>({});

  const cambiar = (campo: keyof FormularioBloqueoValores) => (evento: { target: { value: string } }) =>
    actualizarValores((actuales) => ({ ...actuales, [campo]: evento.target.value }));

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    const resultado = validarBloqueo(valores);
    if (!resultado.valido) {
      actualizarErrores(resultado.errores);
      return;
    }
    actualizarErrores({});
    // El panel informa el error; acá solo se evita el rechazo sin manejar.
    onEnviar(resultado.cuerpo).catch(() => undefined);
  }

  return (
    <form className="agenda-formulario" aria-label="Nuevo bloqueo" noValidate onSubmit={enviar}>
      <Campo etiqueta="Fecha de inicio" type="date" value={valores.fechaInicio} onChange={cambiar("fechaInicio")} />
      <Campo
        etiqueta="Hora de inicio"
        type="time"
        value={valores.horaInicio}
        onChange={cambiar("horaInicio")}
        error={errores.inicio}
      />
      <Campo etiqueta="Fecha de fin" type="date" value={valores.fechaFin} onChange={cambiar("fechaFin")} />
      <Campo etiqueta="Hora de fin" type="time" value={valores.horaFin} onChange={cambiar("horaFin")} error={errores.fin} />
      <div className="agenda-formulario__ancho">
        <Campo
          etiqueta="Motivo"
          value={valores.motivo}
          maxLength={255}
          onChange={cambiar("motivo")}
          error={errores.motivo}
        />
      </div>
      <div className="agenda-formulario__acciones">
        <Boton type="submit" disabled={deshabilitado}>Agregar bloqueo</Boton>
      </div>
    </form>
  );
}
