import { useRef, useState, type FormEvent } from "react";
import { aInstanteUtc } from "../../shared/fecha/zonaHoraria";
import { Alerta, Boton, Campo, Tabla, type ColumnaTabla } from "../../shared/ui";
import { CampoSeleccion } from "../../shared/ui/CampoSeleccion";
import { asignarHueco, consultarCompatibles } from "./api";
import { rechazoDeAsignacion } from "./mensajes";
import { textoFechas, textoFranja } from "./presentacion";
import { Seccion } from "./Seccion";
import type { EntradaRespuesta, HuecoConsultado } from "./tipos";
import type { CatalogosListaEspera } from "./useCatalogosListaEspera";
import { HUECO_VACIO, sinErrores, validarHueco, type DatosHueco, type ErroresHueco } from "./validacion";

type Sugerencias =
  | { tipo: "cargando" }
  | { tipo: "error" }
  | { tipo: "lista"; entradas: readonly EntradaRespuesta[] };

/** Compatibles de un hueco (RN-22) y asignación manual por recepción (US-018, C-43). */
export function SugerenciasHueco({ catalogos, onAsignado }: Readonly<{
  catalogos: CatalogosListaEspera;
  onAsignado: () => void;
}>) {
  const [datos, cambiarDatos] = useState<DatosHueco>(HUECO_VACIO);
  const [errores, cambiarErrores] = useState<ErroresHueco>({});
  const [hueco, cambiarHueco] = useState<HuecoConsultado>();
  const [sugerencias, actualizarSugerencias] = useState<Sugerencias>();
  const [aviso, cambiarAviso] = useState("");
  const [exito, cambiarExito] = useState("");
  const [asignando, cambiarAsignando] = useState(false);
  const enCurso = useRef(false);
  const pedido = useRef(0);

  function actualizar(campo: keyof DatosHueco, valor: string) {
    cambiarDatos((previos) => ({ ...previos, [campo]: valor }));
  }

  async function cargar(consultado: HuecoConsultado) {
    // Solo vale la respuesta de la última consulta.
    const actual = ++pedido.current;
    actualizarSugerencias((previas) => (previas?.tipo === "lista" ? previas : { tipo: "cargando" }));
    try {
      const entradas = await consultarCompatibles(consultado);
      if (actual === pedido.current) actualizarSugerencias({ tipo: "lista", entradas });
    } catch {
      if (actual === pedido.current) actualizarSugerencias({ tipo: "error" });
    }
  }

  function buscar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    const encontrados = validarHueco(datos);
    cambiarErrores(encontrados);
    cambiarAviso("");
    cambiarExito("");
    if (!sinErrores(encontrados)) return;
    // Fecha y horas se interpretan en Buenos Aires (DD-05) y viajan como instantes ISO.
    const consultado: HuecoConsultado = {
      profesionalId: Number(datos.profesionalId),
      inicio: aInstanteUtc(datos.fecha, datos.horaInicio).toISOString(),
      fin: aInstanteUtc(datos.fecha, datos.horaFin).toISOString(),
    };
    cambiarHueco(consultado);
    actualizarSugerencias({ tipo: "cargando" });
    void cargar(consultado);
  }

  async function asignar(entrada: EntradaRespuesta) {
    if (!hueco || enCurso.current) return;
    enCurso.current = true;
    cambiarAsignando(true);
    cambiarAviso("");
    cambiarExito("");
    try {
      await asignarHueco(entrada.id, hueco);
      cambiarExito(`Se asignó el turno a ${catalogos.nombrePaciente(entrada.pacienteId)}.`);
      onAsignado();
      await cargar(hueco);
    } catch (error) {
      const rechazo = rechazoDeAsignacion(error);
      cambiarAviso(rechazo.mensaje);
      if (rechazo.recargar) await cargar(hueco);
    } finally {
      enCurso.current = false;
      cambiarAsignando(false);
    }
  }

  const columnas: readonly ColumnaTabla<EntradaRespuesta>[] = [
    { clave: "paciente", titulo: "Paciente", celda: (e) => catalogos.nombrePaciente(e.pacienteId) },
    { clave: "prestacion", titulo: "Prestación", celda: (e) => catalogos.nombrePrestacion(e.prestacionId) },
    { clave: "fechas", titulo: "Fechas", celda: textoFechas },
    { clave: "franja", titulo: "Franja", celda: textoFranja },
    {
      clave: "acciones",
      titulo: "Acciones",
      celda: (e) => (
        <Boton tamano="chico" disabled={asignando} onClick={() => void asignar(e)}>Asignar</Boton>
      ),
    },
  ];

  return (
    <Seccion titulo="Asignar un hueco">
      <form className="lista-espera__formulario" noValidate aria-label="Hueco a asignar" onSubmit={buscar}>
        <div className="lista-espera__campos">
          <CampoSeleccion etiqueta="Profesional del hueco" vacia="Elegí un profesional" value={datos.profesionalId}
            error={errores.profesionalId}
            opciones={catalogos.profesionales.filter((p) => p.activo)
              .map((p) => ({ valor: String(p.id), etiqueta: `${p.nombre} ${p.apellido}` }))}
            onChange={(evento) => actualizar("profesionalId", evento.target.value)} />
          <Campo etiqueta="Fecha del hueco" type="date" value={datos.fecha} error={errores.fecha}
            onChange={(evento) => actualizar("fecha", evento.target.value)} />
          <Campo etiqueta="Hora de inicio" type="time" value={datos.horaInicio} error={errores.horaInicio}
            onChange={(evento) => actualizar("horaInicio", evento.target.value)} />
          <Campo etiqueta="Hora de fin" type="time" value={datos.horaFin} error={errores.horaFin}
            onChange={(evento) => actualizar("horaFin", evento.target.value)} />
        </div>
        <div>
          <Boton type="submit" variante="secundaria" disabled={asignando}>Buscar compatibles</Boton>
        </div>
      </form>
      {aviso && <Alerta tipo="peligro" titulo={aviso} />}
      {exito && <p role="status" className="lista-espera__exito">{exito}</p>}
      {sugerencias?.tipo === "cargando" && <p role="status">Buscando entradas compatibles…</p>}
      {sugerencias?.tipo === "error" && <Alerta tipo="peligro" titulo="No pudimos buscar entradas compatibles." />}
      {sugerencias?.tipo === "lista" && (
        <Tabla
          titulo="Entradas compatibles con el hueco"
          columnas={columnas}
          filas={sugerencias.entradas}
          claveFila={(e) => e.id}
          mensajeVacio="No hay entradas compatibles con este hueco."
        />
      )}
    </Seccion>
  );
}
