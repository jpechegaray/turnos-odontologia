import { useState, type FormEvent } from "react";
import { api } from "../../shared/api/clienteApi";
import { Alerta, Boton, Campo, Tarjeta, Toast } from "../../shared/ui";
import { CLAVES, type DefinicionClave } from "./claves";
import "./configuracion.css";
import { mensajeDeError } from "./errores";
import type { ClaveConfiguracion, ConfiguracionRespuesta, ConfiguracionTexto } from "./tipos";
import { aSolicitud, aTexto, validarConfiguracion, type ErroresConfiguracion } from "./validacion";

/** Grupos de la pantalla, por el prefijo de cada clave. */
const GRUPOS: readonly Readonly<{ titulo: string; descripcion: string; prefijo: string }>[] = [
  { titulo: "Consultorio", descripcion: "Datos del consultorio y cuánto tiempo se guardan los registros.", prefijo: "consultorio." },
  { titulo: "Turnos", descripcion: "Con cuántas horas de anticipación una cancelación deja de ser tardía.", prefijo: "cancelacion." },
  { titulo: "Recordatorios", descripcion: "Cuántas horas antes del turno se envía el recordatorio por email.", prefijo: "recordatorio." },
  { titulo: "Reserva online", descripcion: "Límites de la reserva que hacen los pacientes desde la web.", prefijo: "reserva_online." },
  { titulo: "Lista de espera", descripcion: "Cuántas personas reciben el aviso de un hueco libre.", prefijo: "lista_espera." },
];

function clavesDe(prefijo: string): readonly DefinicionClave[] {
  return CLAVES.filter((def) => def.clave.startsWith(prefijo));
}

export type FormularioConfiguracionProps = Readonly<{
  valorInicial: ConfiguracionRespuesta;
  soloLectura: boolean;
}>;

export function FormularioConfiguracion({ valorInicial, soloLectura }: FormularioConfiguracionProps) {
  const [valores, actualizarValores] = useState<ConfiguracionTexto>(() => aTexto(valorInicial));
  const [errores, actualizarErrores] = useState<ErroresConfiguracion>({});
  const [errorRemoto, actualizarErrorRemoto] = useState<string>();
  const [aviso, actualizarAviso] = useState<string>();
  const [pendiente, actualizarPendiente] = useState(false);

  function cambiar(clave: ClaveConfiguracion, texto: string) {
    actualizarValores((previos) => ({ ...previos, [clave]: texto }));
    actualizarErrores((previos) => ({ ...previos, [clave]: undefined }));
    actualizarErrorRemoto(undefined);
    actualizarAviso(undefined);
  }

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (soloLectura || pendiente) return;
    const encontrados = validarConfiguracion(valores);
    actualizarErrores(encontrados);
    actualizarErrorRemoto(undefined);
    if (Object.keys(encontrados).length > 0) return;

    actualizarPendiente(true);
    try {
      const resultado = await api.put<ConfiguracionRespuesta>("/configuracion", aSolicitud(valores));
      actualizarValores(aTexto(resultado));
      actualizarAviso("Configuración guardada.");
    } catch (error) {
      actualizarErrorRemoto(mensajeDeError(error));
    } finally {
      actualizarPendiente(false);
    }
  }

  return (
    <Tarjeta titulo="Parámetros del consultorio">
      <form noValidate onSubmit={(evento) => void enviar(evento)}>
        {GRUPOS.map((grupo) => (
          <fieldset key={grupo.prefijo} className="config-grupo">
            <legend className="config-grupo__titulo">{grupo.titulo}</legend>
            <p className="config-grupo__descripcion">{grupo.descripcion}</p>
            <div className="config-grupo__campos">
        {clavesDe(grupo.prefijo).map((def) => (
          <Campo
            key={def.clave}
            etiqueta={def.etiqueta}
            name={def.clave}
            className={def.tipo === "entero" ? "mono" : undefined}
            inputMode={def.tipo === "entero" ? "numeric" : undefined}
            readOnly={soloLectura}
            value={valores[def.clave]}
            onChange={(evento) => cambiar(def.clave, evento.target.value)}
            error={errores[def.clave]}
          />
        ))}
            </div>
          </fieldset>
        ))}
        {errorRemoto && <Alerta tipo="peligro" titulo={errorRemoto} />}
        {aviso && <Toast mensaje={aviso} />}
        {!soloLectura && (
          <Boton type="submit" disabled={pendiente}>Guardar configuración</Boton>
        )}
      </form>
    </Tarjeta>
  );
}
