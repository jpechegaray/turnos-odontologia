import { Alerta, Boton } from "../../shared/ui";
import { FormularioConfiguracion } from "./FormularioConfiguracion";
import { useSoloLecturaConfiguracion } from "./permisos";
import { useConfiguracion } from "./useConfiguracion";

export function PaginaConfiguracion() {
  const soloLectura = useSoloLecturaConfiguracion();
  const { configuracion, cargando, errorCarga, recargar } = useConfiguracion();

  return (
    <section>
      <h2>Configuración</h2>
      {cargando ? (
        <p role="status">Cargando…</p>
      ) : errorCarga ? (
        <Alerta tipo="peligro" titulo="No pudimos cargar la configuración.">
          <Boton variante="secundaria" onClick={() => void recargar()}>Reintentar</Boton>
        </Alerta>
      ) : configuracion ? (
        <FormularioConfiguracion valorInicial={configuracion} soloLectura={soloLectura} />
      ) : null}
    </section>
  );
}
