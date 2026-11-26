import { useEffect, useRef, useState } from "react";
import { ErrorApi } from "../../shared/api/errorApi";
import { Alerta, Boton, Tabla, type ColumnaTabla } from "../../shared/ui";
import { CampoSeleccion } from "../../shared/ui/CampoSeleccion";
import { darDeBaja, listarEntradas } from "./api";
import { MENSAJE_GENERICO } from "./mensajes";
import { textoEstado, textoFechas, textoFranja } from "./presentacion";
import { Seccion } from "./Seccion";
import { ESTADOS, type EntradaRespuesta, type EstadoEntrada } from "./tipos";
import type { CatalogosListaEspera } from "./useCatalogosListaEspera";

type Consulta =
  | { tipo: "cargando" }
  | { tipo: "error" }
  | { tipo: "lista"; entradas: readonly EntradaRespuesta[] };

function BajaEnFila({ onConfirmar, ocupado }: Readonly<{ onConfirmar: () => Promise<void>; ocupado: boolean }>) {
  const [confirmando, cambiarConfirmando] = useState(false);
  if (!confirmando) {
    return (
      <Boton variante="peligro-atenuado" tamano="chico" disabled={ocupado} onClick={() => cambiarConfirmando(true)}>
        Dar de baja
      </Boton>
    );
  }
  return (
    <div className="lista-espera__acciones">
      <Boton variante="peligro" tamano="chico" disabled={ocupado}
        onClick={() => { void onConfirmar().finally(() => cambiarConfirmando(false)); }}>
        Confirmar baja
      </Boton>
      <Boton variante="secundaria" tamano="chico" disabled={ocupado} onClick={() => cambiarConfirmando(false)}>Volver</Boton>
    </div>
  );
}

/** Entradas por estado con nombres legibles y la baja de las activas (US-017). */
export function ListadoEntradas({ catalogos, version }: Readonly<{ catalogos: CatalogosListaEspera; version: number }>) {
  const [estado, cambiarEstado] = useState<EstadoEntrada>("ACTIVA");
  const [recargas, cambiarRecargas] = useState(0);
  const [consulta, actualizarConsulta] = useState<Consulta>({ tipo: "cargando" });
  const [aviso, cambiarAviso] = useState("");
  const [bajando, cambiarBajando] = useState(false);
  const enCurso = useRef(false);

  useEffect(() => {
    let vigente = true;
    actualizarConsulta((previa) => (previa.tipo === "lista" ? previa : { tipo: "cargando" }));
    listarEntradas(estado).then(
      (entradas) => { if (vigente) actualizarConsulta({ tipo: "lista", entradas }); },
      () => { if (vigente) actualizarConsulta({ tipo: "error" }); },
    );
    return () => { vigente = false; };
  }, [estado, version, recargas]);

  async function bajar(entrada: EntradaRespuesta) {
    // Un solo DELETE en curso; sin reintentos automáticos.
    if (enCurso.current) return;
    enCurso.current = true;
    cambiarBajando(true);
    cambiarAviso("");
    try {
      await darDeBaja(entrada.id);
    } catch (error) {
      cambiarAviso(error instanceof ErrorApi && error.codigo === "ENTRADA_NO_ACTIVA"
        ? "La entrada ya no estaba activa. Actualizamos el listado."
        : MENSAJE_GENERICO);
    } finally {
      enCurso.current = false;
      cambiarBajando(false);
      cambiarRecargas((anterior) => anterior + 1);
    }
  }

  const columnas: readonly ColumnaTabla<EntradaRespuesta>[] = [
    { clave: "paciente", titulo: "Paciente", celda: (e) => catalogos.nombrePaciente(e.pacienteId) },
    { clave: "prestacion", titulo: "Prestación", celda: (e) => catalogos.nombrePrestacion(e.prestacionId) },
    { clave: "profesional", titulo: "Profesional", celda: (e) => catalogos.nombreProfesional(e.profesionalId) },
    { clave: "fechas", titulo: "Fechas", celda: textoFechas },
    { clave: "franja", titulo: "Franja", celda: textoFranja },
    { clave: "estado", titulo: "Estado", celda: (e) => textoEstado(e.estado) },
    {
      clave: "acciones",
      titulo: "Acciones",
      celda: (e) => (e.estado === "ACTIVA" ? <BajaEnFila ocupado={bajando} onConfirmar={() => bajar(e)} /> : null),
    },
  ];

  return (
    <Seccion titulo="Entradas">
      <div className="lista-espera__filtros">
        <CampoSeleccion
          etiqueta="Estado"
          value={estado}
          opciones={ESTADOS.map(({ valor, plural }) => ({ valor, etiqueta: plural }))}
          onChange={(evento) => {
            const elegido = ESTADOS.find((opcion) => opcion.valor === evento.target.value);
            if (!elegido) return;
            cambiarAviso("");
            // La tabla queda montada mientras llega el estado nuevo, así el foco y la lectura no saltan.
            cambiarEstado(elegido.valor);
          }}
        />
      </div>
      {aviso && <Alerta tipo="peligro" titulo={aviso} />}
      {consulta.tipo === "cargando" && <p role="status">Cargando la lista de espera…</p>}
      {consulta.tipo === "error" && (
        <Alerta tipo="peligro" titulo="No pudimos cargar la lista de espera.">
          <div>
            <Boton variante="secundaria" tamano="chico" onClick={() => cambiarRecargas((anterior) => anterior + 1)}>
              Reintentar
            </Boton>
          </div>
        </Alerta>
      )}
      {consulta.tipo === "lista" && (
        <Tabla
          titulo="Entradas de la lista de espera"
          columnas={columnas}
          filas={consulta.entradas}
          claveFila={(e) => e.id}
          mensajeVacio="No hay entradas en este estado."
        />
      )}
    </Seccion>
  );
}
