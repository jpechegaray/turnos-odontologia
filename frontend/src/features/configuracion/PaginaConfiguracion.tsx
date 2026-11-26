import { Alerta, Boton } from "../../shared/ui";
import { FormularioConfiguracion } from "./FormularioConfiguracion";
import { useSoloLecturaConfiguracion } from "./permisos";
import { useConfiguracion } from "./useConfiguracion";

export function PaginaConfiguracion() {
  const soloLectura = useSoloLecturaConfiguracion();
  const { configuracion, cargando, errorCarga, recargar } = useConfiguracion();

  return (
    <section>
      <div className="encabezado">
        <div>
          <h2>Configuración</h2>
          <p className="encabezado__bajada">
            {soloLectura ? "Parámetros del consultorio. Solo la administración puede cambiarlos." : "Parámetros del consultorio. Los cambios rigen desde que se guardan."}
          </p>
        </div>
      </div>
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
