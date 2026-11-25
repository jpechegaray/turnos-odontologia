import { useState, type FormEvent } from "react";
import { Boton } from "../../../shared/ui";
import { CampoSeleccion } from "../../../shared/ui/CampoSeleccion";
import type { TurnoAgenda } from "../tipos";
import type { Gestion, MarcaAsistencia } from "./acciones";

const ESTADOS = [
  { valor: "ATENDIDO", etiqueta: "Atendido" },
  { valor: "AUSENTE", etiqueta: "Ausente" },
] as const;

export function FormularioAsistencia({ corregir, turno, bloqueado, onConfirmar }: Readonly<{
  corregir: boolean;
  turno: TurnoAgenda;
  bloqueado: boolean;
  onConfirmar: (gestion: Gestion) => void;
}>) {
  const [marca, elegirMarca] = useState<MarcaAsistencia>();
  const opciones = corregir ? ESTADOS.filter((opcion) => opcion.valor !== turno.estado) : ESTADOS;

  function confirmar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (!marca || bloqueado || !opciones.some((opcion) => opcion.valor === marca)) return;
    onConfirmar({ accion: corregir ? "corregir" : "asistencia", cuerpo: { estado: marca } });
  }

  return (
    <form className="gestion-turnos__formulario" aria-label="Asistencia" onSubmit={confirmar}>
      <CampoSeleccion
        etiqueta="Estado de asistencia"
        opciones={opciones}
        vacia="Elegí un estado"
        value={marca ?? ""}
        disabled={bloqueado}
        onChange={(evento) => elegirMarca(opciones.find((opcion) => opcion.valor === evento.target.value)?.valor)}
      />
      <div className="gestion-turnos__acciones">
        <Boton type="submit" disabled={!marca || bloqueado}>{corregir ? "Guardar corrección" : "Registrar asistencia"}</Boton>
      </div>
    </form>
  );
}
