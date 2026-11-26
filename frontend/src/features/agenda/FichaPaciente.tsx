import { useEffect, useId, useState } from "react";
import { api } from "../../shared/api/clienteApi";
import { fechaLocal } from "../../shared/fecha/zonaHoraria";
import { Alerta, Avatar, BadgeEstadoTurno, Icono } from "../../shared/ui";
import type { PacienteRespuesta } from "../pacientes/tipos";
import type { FichaElegida } from "./ContenidoTurno";
import { fechaCorta, rangoLegible, type Nombres } from "./presentacion";

const MARCA_VACIO = "—";

type Estado =
  | Readonly<{ tipo: "cargando" }>
  | Readonly<{ tipo: "lista"; ficha: PacienteRespuesta }>
  | Readonly<{ tipo: "error" }>;

/** "03/2025" a partir del instante de alta. */
function mesDeAlta(creadoEn: string): string {
  return fechaCorta(fechaLocal(creadoEn), true).slice(3);
}

/**
 * Ficha básica del paciente en solo lectura (US-013), al costado de la agenda. Para el odontólogo, la API
 * solo la entrega si el paciente tiene turnos con su profesional (RN-33); si no, se muestra el error.
 */
export function FichaPaciente({ elegida, nombres, onCerrar }: Readonly<{ elegida: FichaElegida; nombres: Nombres; onCerrar: () => void }>) {
  const [estado, actualizarEstado] = useState<Estado>({ tipo: "cargando" });
  const idTitulo = useId();
  const turno = elegida.turno;

  useEffect(() => {
    let vigente = true;
    api.get<PacienteRespuesta>(`/pacientes/${elegida.pacienteId}`).then(
      (ficha) => vigente && actualizarEstado({ tipo: "lista", ficha }),
      () => vigente && actualizarEstado({ tipo: "error" }),
    );
    return () => {
      vigente = false;
    };
  }, [elegida.pacienteId]);

  return (
    <section className="panel panel--plano ficha" aria-label={`Ficha de ${elegida.nombre}`}>
      <div className="ficha__cabeza">
        <Avatar nombre={elegida.nombre} tamano="lg" />
        <div className="ficha__titulo">
          <h3 id={idTitulo}>{elegida.nombre}</h3>
          {estado.tipo === "lista" && estado.ficha.creadoEn && (
            <span className="muted">{`Paciente desde ${mesDeAlta(estado.ficha.creadoEn)}`}</span>
          )}
        </div>
        <button type="button" className="boton-icono" aria-label="Cerrar" onClick={onCerrar}>
          <Icono nombre="x" />
        </button>
      </div>
      {turno && (
        <div className="ficha__turno">
          <div className="ficha__linea">
            <span className="rotulo">Turno seleccionado</span>
            <BadgeEstadoTurno estado={turno.estado} />
          </div>
          <strong className="mono">{rangoLegible(turno.inicio, turno.fin)}</strong>
          <span className="muted">
            {`${nombres.prestacion(turno.prestacionId)} · ${nombres.profesional(turno.profesionalId)} · ${nombres.sillon(turno.sillonId)}`}
          </span>
        </div>
      )}
      <div className="ficha__cuerpo">
        {estado.tipo === "cargando" && <p role="status">Cargando…</p>}
        {estado.tipo === "error" && <Alerta tipo="peligro" titulo="No pudimos cargar la ficha del paciente." />}
        {estado.tipo === "lista" && (
          <dl className="datos">
            <dt>DNI</dt>
            <dd className="mono">{estado.ficha.dni ?? MARCA_VACIO}</dd>
            <dt>Teléfono</dt>
            <dd className="mono">{estado.ficha.telefono ?? MARCA_VACIO}</dd>
            <dt>Email</dt>
            <dd>{estado.ficha.email ?? MARCA_VACIO}</dd>
            <dt>Obra social</dt>
            <dd>{estado.ficha.obraSocial ?? MARCA_VACIO}</dd>
            <dt>N.º de afiliado</dt>
            <dd className="mono">{estado.ficha.nroAfiliado ?? MARCA_VACIO}</dd>
            {estado.ficha.notas && (
              <>
                <dt>Notas</dt>
                <dd>{estado.ficha.notas}</dd>
              </>
            )}
          </dl>
        )}
      </div>
    </section>
  );
}
