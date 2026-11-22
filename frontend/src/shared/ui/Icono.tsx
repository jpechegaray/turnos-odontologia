import type { ReactElement } from "react";

/** Trazos copiados de los símbolos de design-system/components.html. */
const TRAZOS = {
  reloj: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </>
  ),
  check: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="m8 12.5 3 3 5-6" />
    </>
  ),
  hecho: (
    <>
      <path d="m3 12.5 4 4 7-9" />
      <path d="m11 16.5 1 1 9-10.5" />
    </>
  ),
  ausente: (
    <>
      <circle cx="10" cy="8" r="4" />
      <path d="M3 20c0-3.5 3-6 7-6 1.3 0 2.5.3 3.5.8" />
      <path d="m17 14 4 4m0-4-4 4" />
    </>
  ),
  cruz: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="m9 9 6 6m0-6-6 6" />
    </>
  ),
  alerta: (
    <>
      <path d="M12 3 2 20h20L12 3z" />
      <path d="M12 10v4m0 3v.01" />
    </>
  ),
  candado: (
    <>
      <rect x="5" y="11" width="14" height="9" rx="2" />
      <path d="M8 11V8a4 4 0 0 1 8 0v3" />
    </>
  ),
  info: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 11v5m0-8v.01" />
    </>
  ),
} satisfies Record<string, ReactElement>;

export type NombreIcono = keyof typeof TRAZOS;

/** Siempre decorativo: el texto que lo acompaña transmite el significado. */
export function Icono({ nombre }: { nombre: NombreIcono }) {
  return (
    <svg className="icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      {TRAZOS[nombre]}
    </svg>
  );
}
