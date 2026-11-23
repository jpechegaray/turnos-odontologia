import { useState, type FormEvent } from "react";
import { Alerta, Boton, Campo, Tarjeta } from "../../shared/ui";
import { mensajeDeError } from "./errores";
import type { PrestacionRespuesta, PrestacionSolicitud } from "./tipos";
import { enteroEstricto, validarPrestacion, type ErroresPrestacion } from "./validacion";

export type FormularioPrestacionProps = Readonly<{
  valorInicial?: PrestacionRespuesta;
  etiquetaEnvio?: string;
  onEnviar: (solicitud: PrestacionSolicitud) => Promise<void>;
  onCancelar?: () => void;
}>;

export function FormularioPrestacion({
  valorInicial,
  etiquetaEnvio = "Agregar prestación",
  onEnviar,
  onCancelar,
}: FormularioPrestacionProps) {
  const [nombre, actualizarNombre] = useState(valorInicial?.nombre ?? "");
  const [duracion, actualizarDuracion] = useState(
    valorInicial ? String(valorInicial.duracionDefaultMin) : "",
  );
  const [errores, actualizarErrores] = useState<ErroresPrestacion>({});
  const [errorRemoto, actualizarErrorRemoto] = useState<string>();
  const [pendiente, actualizarPendiente] = useState(false);

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (pendiente) return;
    const encontrados = validarPrestacion(nombre, duracion);
    actualizarErrores(encontrados);
    actualizarErrorRemoto(undefined);
    const duracionMin = enteroEstricto(duracion);
    if (encontrados.nombre || encontrados.duracion || duracionMin === undefined) return;

    actualizarPendiente(true);
    try {
      await onEnviar({ nombre: nombre.trim(), duracionDefaultMin: duracionMin });
      actualizarNombre("");
      actualizarDuracion("");
      actualizarErrores({});
    } catch (error) {
      actualizarErrorRemoto(mensajeDeError(error));
    } finally {
      actualizarPendiente(false);
    }
  }

  return (
    <Tarjeta titulo={valorInicial ? "Editar prestación" : "Nueva prestación"}>
      <form noValidate onSubmit={(evento) => void enviar(evento)}>
        <Campo
          etiqueta="Nombre"
          name="nombre"
          value={nombre}
          onChange={(evento) => {
            actualizarNombre(evento.target.value);
            actualizarErrores((previos) => ({ ...previos, nombre: undefined }));
          }}
          error={errores.nombre}
        />
        <Campo
          etiqueta="Duración (min)"
          name="duracion"
          inputMode="numeric"
          value={duracion}
          onChange={(evento) => {
            actualizarDuracion(evento.target.value);
            actualizarErrores((previos) => ({ ...previos, duracion: undefined }));
          }}
          error={errores.duracion}
        />
        {errorRemoto && <Alerta tipo="peligro" titulo={errorRemoto} />}
        <Boton type="submit" disabled={pendiente}>{etiquetaEnvio}</Boton>
        {onCancelar && (
          <Boton variante="secundaria" onClick={onCancelar}>Cancelar</Boton>
        )}
      </form>
    </Tarjeta>
  );
}
