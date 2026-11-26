import { useRef, useState, type FormEvent } from "react";
import { Alerta, Boton, Campo } from "../../shared/ui";
import { CampoSeleccion } from "../../shared/ui/CampoSeleccion";
import { SeleccionPaciente } from "../agenda/crear/SeleccionPaciente";
import { crearEntrada } from "./api";
import { rechazoDelAlta } from "./mensajes";
import { Seccion } from "./Seccion";
import type { CatalogosListaEspera } from "./useCatalogosListaEspera";
import { ALTA_VACIA, sinErrores, validarAlta, type DatosAlta, type ErroresAlta } from "./validacion";

function nuloSiVacio(valor: string) {
  return valor === "" ? null : valor;
}

/** Anotar un paciente en la lista de espera (US-017, RN-21). */
export function FormularioAlta({ catalogos, onAnotado }: Readonly<{
  catalogos: CatalogosListaEspera;
  onAnotado: () => void;
}>) {
  const [datos, cambiarDatos] = useState<DatosAlta>(ALTA_VACIA);
  const [errores, cambiarErrores] = useState<ErroresAlta>({});
  const [aviso, cambiarAviso] = useState("");
  const [exito, cambiarExito] = useState("");
  const [enviando, cambiarEnviando] = useState(false);
  const [busquedaPaciente, cambiarBusquedaPaciente] = useState(0);
  const enCurso = useRef(false);

  function actualizar(campo: keyof DatosAlta, valor: string) {
    cambiarDatos((previos) => ({ ...previos, [campo]: valor }));
    cambiarExito("");
  }

  async function anotar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (enCurso.current) return;
    const encontrados = validarAlta(datos);
    cambiarErrores(encontrados);
    cambiarAviso("");
    cambiarExito("");
    if (!sinErrores(encontrados)) return;
    enCurso.current = true;
    cambiarEnviando(true);
    try {
      await crearEntrada({
        pacienteId: Number(datos.pacienteId),
        prestacionId: Number(datos.prestacionId),
        profesionalId: datos.profesionalId ? Number(datos.profesionalId) : null,
        fechaDesde: nuloSiVacio(datos.fechaDesde),
        fechaHasta: datos.fechaHasta,
        franjaDesde: nuloSiVacio(datos.franjaDesde),
        franjaHasta: nuloSiVacio(datos.franjaHasta),
      });
      cambiarDatos(ALTA_VACIA);
      cambiarBusquedaPaciente((anterior) => anterior + 1);
      cambiarExito("El paciente quedó anotado en la lista de espera.");
      onAnotado();
    } catch (error) {
      // El formulario se conserva para corregir y volver a enviar.
      const rechazo = rechazoDelAlta(error);
      cambiarErrores(rechazo.campos);
      cambiarAviso(rechazo.mensaje);
    } finally {
      enCurso.current = false;
      cambiarEnviando(false);
    }
  }

  return (
    <Seccion titulo="Anotar paciente">
      <SeleccionPaciente key={busquedaPaciente} pacienteId={datos.pacienteId} bloqueada={enviando}
        onElegir={(id) => actualizar("pacienteId", id)} />
      {errores.pacienteId && <p className="error">{errores.pacienteId}</p>}
      <form className="lista-espera__formulario" noValidate aria-label="Datos de la entrada" onSubmit={(evento) => void anotar(evento)}>
        <div className="lista-espera__campos">
          <CampoSeleccion etiqueta="Prestación" vacia="Elegí una prestación" value={datos.prestacionId}
            error={errores.prestacionId} disabled={enviando}
            opciones={catalogos.prestaciones.filter((p) => p.activa).map((p) => ({ valor: String(p.id), etiqueta: p.nombre }))}
            onChange={(evento) => actualizar("prestacionId", evento.target.value)} />
          <CampoSeleccion etiqueta="Profesional preferido" vacia="Cualquiera" value={datos.profesionalId} disabled={enviando}
            opciones={catalogos.profesionales.filter((p) => p.activo)
              .map((p) => ({ valor: String(p.id), etiqueta: `${p.nombre} ${p.apellido}` }))}
            onChange={(evento) => actualizar("profesionalId", evento.target.value)} />
          <Campo etiqueta="Desde" type="date" ayuda="Opcional" value={datos.fechaDesde} error={errores.fechaDesde}
            disabled={enviando} onChange={(evento) => actualizar("fechaDesde", evento.target.value)} />
          <Campo etiqueta="Hasta" type="date" value={datos.fechaHasta} error={errores.fechaHasta} disabled={enviando}
            onChange={(evento) => actualizar("fechaHasta", evento.target.value)} />
          <Campo etiqueta="Franja desde" type="time" ayuda="Opcional" value={datos.franjaDesde} disabled={enviando}
            onChange={(evento) => actualizar("franjaDesde", evento.target.value)} />
          <Campo etiqueta="Franja hasta" type="time" ayuda="Opcional" value={datos.franjaHasta} error={errores.franjaHasta}
            disabled={enviando} onChange={(evento) => actualizar("franjaHasta", evento.target.value)} />
        </div>
        {aviso && <Alerta tipo="peligro" titulo={aviso} />}
        {exito && <p role="status" className="lista-espera__exito">{exito}</p>}
        <div>
          <Boton type="submit" disabled={enviando}>Anotar en la lista</Boton>
        </div>
      </form>
    </Seccion>
  );
}
