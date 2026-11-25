import { useEffect, useId, useState } from "react";
import { api } from "../../shared/api/clienteApi";
import { Alerta, Boton } from "../../shared/ui";
import type { PacienteRespuesta } from "../pacientes/tipos";
import type { FichaElegida } from "./ContenidoTurno";

const MARCA_VACIO = "—";

type Estado =
  | Readonly<{ tipo: "cargando" }>
  | Readonly<{ tipo: "lista"; ficha: PacienteRespuesta }>
  | Readonly<{ tipo: "error" }>;

/**
 * Ficha básica del paciente en solo lectura (US-013). Para el odontólogo, la API solo la entrega si el
 * paciente tiene turnos con su profesional (RN-33); si no, se muestra el error.
 */
export function FichaPaciente({ elegida, onCerrar }: Readonly<{ elegida: FichaElegida; onCerrar: () => void }>) {
  const [estado, actualizarEstado] = useState<Estado>({ tipo: "cargando" });
  const idTitulo = useId();

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
    <section className="panel ficha-paciente" aria-labelledby={idTitulo}>
      <h3 id={idTitulo} className="panel__titulo">{`Ficha de ${elegida.nombre}`}</h3>
      {estado.tipo === "cargando" && <p role="status">Cargando…</p>}
      {estado.tipo === "error" && <Alerta tipo="peligro" titulo="No pudimos cargar la ficha del paciente." />}
      {estado.tipo === "lista" && (
        <dl className="ficha-paciente__datos">
          <dt>Nombre</dt>
          <dd>{`${estado.ficha.nombre} ${estado.ficha.apellido}`}</dd>
          <dt>DNI</dt>
          <dd>{estado.ficha.dni ?? MARCA_VACIO}</dd>
          <dt>Teléfono</dt>
          <dd>{estado.ficha.telefono ?? MARCA_VACIO}</dd>
          <dt>Email</dt>
          <dd>{estado.ficha.email ?? MARCA_VACIO}</dd>
          <dt>Obra social</dt>
          <dd>{estado.ficha.obraSocial ?? MARCA_VACIO}</dd>
          <dt>N.º de afiliado</dt>
          <dd>{estado.ficha.nroAfiliado ?? MARCA_VACIO}</dd>
        </dl>
      )}
      <Boton variante="secundaria" tamano="chico" onClick={onCerrar}>Cerrar</Boton>
    </section>
  );
}
