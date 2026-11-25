import { useState, type FormEvent } from "react";
import { Alerta, Boton, Campo } from "../../shared/ui";
import type { CampoPaciente, Consultorio, DatosPaciente } from "./reservaApi";

export const VERSION_AVISO = "2026-11-v1";
export type ErroresPaciente = Partial<Record<CampoPaciente, string>>;

const campos = [
  { nombre: "nombre", etiqueta: "Nombre", autoComplete: "given-name", maxLength: 100 },
  { nombre: "apellido", etiqueta: "Apellido", autoComplete: "family-name", maxLength: 100 },
  { nombre: "email", etiqueta: "Email", autoComplete: "email", type: "email", maxLength: 254 },
  { nombre: "telefono", etiqueta: "Teléfono", autoComplete: "tel", type: "tel", maxLength: 30 },
  { nombre: "obraSocial", etiqueta: "Obra social", maxLength: 100, placeholder: "Particular, si no tenés" },
] as const;

function AvisoDatos({ consultorio }: { consultorio: Consultorio }) {
  return (
    <details className="reserva-aviso">
      <summary>Cómo usamos tus datos</summary>
      <p><strong>Responsable.</strong> {consultorio.nombre}, {consultorio.domicilio}, {consultorio.emailContacto}, es responsable de la base de datos de pacientes.</p>
      <p><strong>Para qué.</strong> Usamos tu nombre, email, teléfono y obra social solo para registrar tu turno, enviarte la confirmación con el enlace para gestionarlo, recordatorios y, si te anotás en la lista de espera, avisos de horarios libres. No los usamos para publicidad.</p>
      <p><strong>Quién los ve.</strong> Solo el personal del consultorio. Los emails se envían por un proveedor de correo que actúa por cuenta nuestra. No cedemos tus datos a terceros.</p>
      <p><strong>Son necesarios.</strong> Sin estos datos no podemos reservar el turno ni avisarte cambios. Si son inexactos, puede que no recibas la confirmación.</p>
      <p><strong>Tus derechos.</strong> Podés pedir acceso a tus datos, corregirlos o suprimirlos escribiendo a {consultorio.emailContacto}. El acceso es gratuito a intervalos no inferiores a seis meses, salvo interés legítimo (art. 14 inc. 3, Ley 25.326). La Agencia de Acceso a la Información Pública, órgano de control de la Ley 25.326, atiende las denuncias y reclamos por incumplimiento de las normas de protección de datos personales.</p>
      <p><strong>Cuánto tiempo.</strong> Conservamos tus datos mientras seas paciente y hasta {consultorio.retencionAnios} {consultorio.retencionAnios === 1 ? "año" : "años"} después de tu último turno.</p>
    </details>
  );
}

type Props = {
  datos: DatosPaciente;
  actualizar: (campo: CampoPaciente, valor: string) => void;
  errores: ErroresPaciente;
  consultorio?: Consultorio;
  aceptado: boolean;
  aceptar: (valor: boolean) => void;
  enviando: boolean;
  confirmar: () => void;
};

export function FormularioReserva({ datos, actualizar, errores, consultorio, aceptado, aceptar, enviando, confirmar }: Props) {
  const [locales, fijarLocales] = useState<ErroresPaciente>({});
  const [faltaAceptacion, fijarFaltaAceptacion] = useState(false);
  const vigente = consultorio?.versionConsentimiento === VERSION_AVISO;

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    const form = evento.currentTarget;
    const incidencias: ErroresPaciente = {};
    for (const { nombre } of campos) {
      const elemento = form.elements.namedItem(nombre);
      if (!datos[nombre].trim()) incidencias[nombre] = "Completá este campo.";
      else if (elemento instanceof HTMLInputElement && elemento.validity.typeMismatch) {
        incidencias[nombre] = "Ingresá un email válido.";
      }
    }
    fijarLocales(incidencias);
    fijarFaltaAceptacion(!aceptado);
    const primerInvalido = Object.keys(incidencias)[0] ?? (!aceptado ? "consentimiento" : undefined);
    if (primerInvalido) {
      const campo = form.elements.namedItem(primerInvalido);
      if (campo instanceof HTMLElement) campo.focus();
      return;
    }
    confirmar();
  }

  return (
    <form className="reserva-formulario" onSubmit={enviar} noValidate>
      {campos.map(({ nombre, ...campo }) => (
        <Campo key={nombre} {...campo} name={nombre} value={datos[nombre]}
          error={locales[nombre] ?? errores[nombre]} required disabled={enviando}
          onChange={(evento) => {
            fijarLocales((anterior) => ({ ...anterior, [nombre]: undefined }));
            actualizar(nombre, evento.target.value);
          }} />
      ))}
      {consultorio && !vigente && <Alerta tipo="peligro" titulo="El consentimiento vigente no está disponible." />}
      {consultorio && vigente && (
        <>
          <AvisoDatos consultorio={consultorio} />
          <label className="reserva-consentimiento">
            <input type="checkbox" name="consentimiento" required disabled={enviando} checked={aceptado}
              aria-invalid={faltaAceptacion || undefined}
              aria-describedby={faltaAceptacion ? "reserva-error-consentimiento" : undefined}
              onChange={(evento) => {
                fijarFaltaAceptacion(false);
                aceptar(evento.target.checked);
              }} />
            <span>Leí y acepto que {consultorio.nombre} trate mis datos personales para gestionar mis turnos, como se explica en <em>Cómo usamos tus datos</em>.</span>
          </label>
          {faltaAceptacion && <p id="reserva-error-consentimiento" role="alert">Aceptá el tratamiento de tus datos para reservar.</p>}
        </>
      )}
      <Boton type="submit" anchoCompleto disabled={enviando || !vigente}>
        {enviando ? "Reservando…" : "Reservar turno"}
      </Boton>
    </form>
  );
}
