import { useState, type FormEvent } from "react";
import { api } from "../../shared/api/clienteApi";
import { Alerta, Boton, Campo, Tarjeta, Toast } from "../../shared/ui";
import { CLAVES } from "./claves";
import { mensajeDeError } from "./errores";
import type { ClaveConfiguracion, ConfiguracionRespuesta, ConfiguracionTexto } from "./tipos";
import { aSolicitud, aTexto, validarConfiguracion, type ErroresConfiguracion } from "./validacion";

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
        {CLAVES.map((def) => (
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
        {errorRemoto && <Alerta tipo="peligro" titulo={errorRemoto} />}
        {aviso && <Toast mensaje={aviso} />}
        {!soloLectura && (
          <Boton type="submit" disabled={pendiente}>Guardar configuración</Boton>
        )}
      </form>
    </Tarjeta>
  );
}
