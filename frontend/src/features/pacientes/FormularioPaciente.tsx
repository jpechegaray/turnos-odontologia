import { useId, useState, type FormEvent } from "react";
import { Alerta, Boton, Campo, Icono, Tarjeta } from "../../shared/ui";
import { erroresDeApiPaciente, type ErroresDeApi } from "./errores";
import type { CampoPaciente, PacienteRespuesta, PacienteSolicitud } from "./tipos";
import { normalizarPaciente, validarPaciente, type ErroresPaciente, type ValoresPaciente } from "./validacion";

const VALORES_VACIOS: ValoresPaciente = {
  nombre: "",
  apellido: "",
  dni: "",
  email: "",
  telefono: "",
  obraSocial: "",
  nroAfiliado: "",
  notas: "",
};

function valoresDe(paciente?: PacienteRespuesta): ValoresPaciente {
  if (!paciente) return VALORES_VACIOS;
  return {
    nombre: paciente.nombre,
    apellido: paciente.apellido,
    dni: paciente.dni ?? "",
    email: paciente.email ?? "",
    telefono: paciente.telefono ?? "",
    obraSocial: paciente.obraSocial ?? "",
    nroAfiliado: paciente.nroAfiliado ?? "",
    notas: paciente.notas ?? "",
  };
}

type CampoDeTexto = Readonly<{ campo: Exclude<CampoPaciente, "notas">; etiqueta: string; maxLength: number }>;

const CAMPOS: readonly CampoDeTexto[] = [
  { campo: "nombre", etiqueta: "Nombre", maxLength: 50 },
  { campo: "apellido", etiqueta: "Apellido", maxLength: 50 },
  { campo: "dni", etiqueta: "DNI", maxLength: 20 },
  { campo: "email", etiqueta: "Email", maxLength: 100 },
  { campo: "telefono", etiqueta: "Teléfono", maxLength: 20 },
  { campo: "obraSocial", etiqueta: "Obra social", maxLength: 200 },
  { campo: "nroAfiliado", etiqueta: "Número de afiliado", maxLength: 50 },
];

const LARGO_MAXIMO_NOTAS = 1000;

/** Errores de validación local por campo; el de contacto aparece en email y teléfono. */
function erroresLocalesPorCampo(errores: ErroresPaciente): Readonly<Partial<Record<CampoPaciente, string>>> {
  return {
    nombre: errores.nombre,
    apellido: errores.apellido,
    email: errores.contacto,
    telefono: errores.contacto,
  };
}

type AreaNotasProps = Readonly<{
  value: string;
  maxLength: number;
  error?: string;
  onChange: (valor: string) => void;
}>;

/** El kit no tiene área de texto: usa las clases y el cableado aria de `Campo` (design D6). */
function AreaNotas({ value, maxLength, error, onChange }: AreaNotasProps) {
  const id = useId();
  const idError = `${id}-error`;
  return (
    <div className="field">
      <label htmlFor={id}>Notas</label>
      <textarea
        id={id}
        className="input"
        name="notas"
        value={value}
        maxLength={maxLength}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? idError : undefined}
        onChange={(evento) => onChange(evento.target.value)}
      />
      {error && (
        <span id={idError} className="error">
          <Icono nombre="alerta" />
          {error}
        </span>
      )}
    </div>
  );
}

export type FormularioPacienteProps = Readonly<{
  valorInicial?: PacienteRespuesta;
  etiquetaEnvio?: string;
  onEnviar: (solicitud: PacienteSolicitud) => Promise<void>;
  onCancelar?: () => void;
}>;

export function FormularioPaciente({
  valorInicial,
  etiquetaEnvio = "Agregar paciente",
  onEnviar,
  onCancelar,
}: FormularioPacienteProps) {
  const [valores, actualizarValores] = useState<ValoresPaciente>(() => valoresDe(valorInicial));
  const [locales, actualizarLocales] = useState<Readonly<Partial<Record<CampoPaciente, string>>>>({});
  const [remotos, actualizarRemotos] = useState<ErroresDeApi>({ campos: {} });
  const [pendiente, actualizarPendiente] = useState(false);

  /** Editar un campo limpia sus errores; los de contacto se limpian juntos. */
  function cambiar(campo: CampoPaciente, valor: string) {
    const afectados: readonly CampoPaciente[] = campo === "email" || campo === "telefono" ? ["email", "telefono"] : [campo];
    const sinErrores = (previos: Readonly<Partial<Record<CampoPaciente, string>>>) => ({
      ...previos,
      ...Object.fromEntries(afectados.map((afectado) => [afectado, undefined])),
    });
    actualizarValores((previos) => ({ ...previos, [campo]: valor }));
    actualizarLocales(sinErrores);
    actualizarRemotos((previos) => ({ ...previos, campos: sinErrores(previos.campos) }));
  }

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (pendiente) return;
    const encontrados = validarPaciente(valores);
    const porCampo = erroresLocalesPorCampo(encontrados);
    actualizarLocales(porCampo);
    actualizarRemotos({ campos: {} });
    if (Object.values(porCampo).some(Boolean)) return;

    actualizarPendiente(true);
    try {
      await onEnviar(normalizarPaciente(valores));
      actualizarValores(VALORES_VACIOS);
    } catch (error) {
      actualizarRemotos(erroresDeApiPaciente(error));
    } finally {
      actualizarPendiente(false);
    }
  }

  function errorDe(campo: CampoPaciente): string | undefined {
    return locales[campo] ?? remotos.campos[campo];
  }

  return (
    <Tarjeta titulo={valorInicial ? "Editar paciente" : "Nuevo paciente"}>
      <form aria-label="Datos del paciente" noValidate onSubmit={(evento) => void enviar(evento)}>
        <div className="form-grilla">
        {CAMPOS.map(({ campo, etiqueta, maxLength }) => (
          <Campo
            key={campo}
            etiqueta={etiqueta}
            name={campo}
            maxLength={maxLength}
            value={valores[campo]}
            onChange={(evento) => cambiar(campo, evento.target.value)}
            error={errorDe(campo)}
          />
        ))}
        </div>
        <AreaNotas
          value={valores.notas}
          maxLength={LARGO_MAXIMO_NOTAS}
          error={errorDe("notas")}
          onChange={(valor) => cambiar("notas", valor)}
        />
        {remotos.alerta ? <Alerta tipo="peligro" titulo={remotos.alerta} /> : null}
        <div className="form-acciones">
          {onCancelar ? <Boton variante="secundaria" onClick={onCancelar}>Cancelar</Boton> : null}
          <Boton type="submit" disabled={pendiente}>{etiquetaEnvio}</Boton>
        </div>
      </form>
    </Tarjeta>
  );
}
