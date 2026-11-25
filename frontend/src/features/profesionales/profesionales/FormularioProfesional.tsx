import { useState, type FormEvent } from "react";
import { Alerta, Boton, Campo, Tarjeta } from "../../../shared/ui";
import { mensajeDeError } from "./errores";
import type { ProfesionalRespuesta, ProfesionalSolicitud } from "./tipos";
import {
  hayErrores,
  solicitudDesde,
  validarProfesional,
  valoresDesde,
  VALORES_VACIOS,
  type ClaveCampoProfesional,
  type ErroresProfesional,
  type ValoresProfesional,
} from "./validacion";

export type FormularioProfesionalProps = Readonly<{
  /** Con un profesional, el formulario edita; sin él, da de alta. La página lo monta con `key` por id. */
  valorInicial?: ProfesionalRespuesta;
  onEnviar: (solicitud: ProfesionalSolicitud) => Promise<void>;
  onCancelar?: () => void;
}>;

export function FormularioProfesional({ valorInicial, onEnviar, onCancelar }: FormularioProfesionalProps) {
  const [valores, actualizarValores] = useState<ValoresProfesional>(() =>
    valorInicial ? valoresDesde(valorInicial) : VALORES_VACIOS,
  );
  const [errores, actualizarErrores] = useState<ErroresProfesional>({});
  const [errorRemoto, actualizarErrorRemoto] = useState<string>();
  const [pendiente, actualizarPendiente] = useState(false);

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (pendiente) return;
    const encontrados = validarProfesional(valores);
    actualizarErrores(encontrados);
    actualizarErrorRemoto(undefined);
    if (hayErrores(encontrados)) return;

    actualizarPendiente(true);
    try {
      await onEnviar(solicitudDesde(valores));
      actualizarValores(VALORES_VACIOS);
    } catch (error) {
      actualizarErrorRemoto(mensajeDeError(error));
    } finally {
      actualizarPendiente(false);
    }
  }

  function campo(clave: ClaveCampoProfesional, etiqueta: string) {
    return (
      <Campo
        etiqueta={etiqueta}
        name={clave}
        value={valores[clave]}
        onChange={(evento) => {
          const valor = evento.target.value;
          actualizarValores((previos) => ({ ...previos, [clave]: valor }));
          actualizarErrores((previos) => ({ ...previos, [clave]: undefined }));
        }}
        error={errores[clave]}
      />
    );
  }

  return (
    <Tarjeta titulo={valorInicial ? "Editar profesional" : "Nuevo profesional"}>
      <form noValidate onSubmit={(evento) => void enviar(evento)}>
        {campo("nombre", "Nombre")}
        {campo("apellido", "Apellido")}
        {campo("matricula", "Matrícula")}
        {campo("especialidad", "Especialidad")}
        {errorRemoto && <Alerta tipo="peligro" titulo={errorRemoto} />}
        <Boton type="submit" disabled={pendiente}>
          {valorInicial ? "Guardar cambios" : "Agregar profesional"}
        </Boton>
        {onCancelar && (
          <Boton variante="secundaria" onClick={onCancelar}>Cancelar</Boton>
        )}
      </form>
    </Tarjeta>
  );
}
