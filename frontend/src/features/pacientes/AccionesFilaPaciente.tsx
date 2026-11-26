import { Boton, Icono } from "../../shared/ui";
import { useEsAdministrador } from "./permisos";
import type { PacienteRespuesta } from "./tipos";

export type AccionesFilaPacienteProps = Readonly<{
  paciente: PacienteRespuesta;
  confirmandoBaja: boolean;
  onEditar: (paciente: PacienteRespuesta) => void;
  onSolicitarBaja: (paciente: PacienteRespuesta) => void;
  onConfirmarBaja: (paciente: PacienteRespuesta) => void;
  onCancelarBaja: () => void;
}>;

/** Acciones de una fila; la baja es solo del administrador y pide confirmación en la propia fila. */
export function AccionesFilaPaciente({
  paciente,
  confirmandoBaja,
  onEditar,
  onSolicitarBaja,
  onConfirmarBaja,
  onCancelarBaja,
}: AccionesFilaPacienteProps) {
  const esAdmin = useEsAdministrador();

  if (confirmandoBaja) {
    return (
      <div>
        <Boton variante="peligro" tamano="chico" onClick={() => onConfirmarBaja(paciente)}>Confirmar baja</Boton>
        <Boton variante="secundaria" tamano="chico" onClick={onCancelarBaja}>Cancelar</Boton>
      </div>
    );
  }

  return (
    <div>
      <Boton variante="secundaria" tamano="chico" onClick={() => onEditar(paciente)}><Icono nombre="editar" />Editar</Boton>
      {esAdmin ? (
        <Boton variante="peligro-atenuado" tamano="chico" onClick={() => onSolicitarBaja(paciente)}>
          Dar de baja
        </Boton>
      ) : null}
    </div>
  );
}
