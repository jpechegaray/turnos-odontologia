import { useState, type FormEvent } from "react";
import { Alerta, Boton, Campo, Tarjeta } from "../../../shared/ui";
import { mensajeDeErrorSillon } from "./errores";
import type { SillonRespuesta, SillonSolicitud } from "./tipos";
import { normalizarSillon, validarSillon, type ErroresSillon } from "./validacion";

export type FormularioSillonProps = Readonly<{
  valorInicial?: SillonRespuesta;
  etiquetaEnvio?: string;
  onEnviar: (solicitud: SillonSolicitud) => Promise<void>;
  onCancelar?: () => void;
}>;

export function FormularioSillon({
  valorInicial,
  etiquetaEnvio = "Agregar sillón",
  onEnviar,
  onCancelar,
}: FormularioSillonProps) {
  const [nombre, actualizarNombre] = useState(valorInicial?.nombre ?? "");
  const [descripcion, actualizarDescripcion] = useState(valorInicial?.descripcion ?? "");
  const [errores, actualizarErrores] = useState<ErroresSillon>({});
  const [errorRemoto, actualizarErrorRemoto] = useState<string>();
  const [pendiente, actualizarPendiente] = useState(false);

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (pendiente) return;
    const encontrados = validarSillon(nombre);
    actualizarErrores(encontrados);
    actualizarErrorRemoto(undefined);
    if (encontrados.nombre) return;

    actualizarPendiente(true);
    try {
      await onEnviar(normalizarSillon(nombre, descripcion));
      actualizarNombre("");
      actualizarDescripcion("");
      actualizarErrores({});
    } catch (error) {
      actualizarErrorRemoto(mensajeDeErrorSillon(error));
    } finally {
      actualizarPendiente(false);
    }
  }

  return (
    <Tarjeta titulo={valorInicial ? "Editar sillón" : "Nuevo sillón"}>
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
          etiqueta="Descripción"
          name="descripcion"
          value={descripcion}
          onChange={(evento) => actualizarDescripcion(evento.target.value)}
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
