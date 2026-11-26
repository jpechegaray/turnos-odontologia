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
  inicio: <path d="M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z" />,
  agenda: (
    <>
      <rect x="3" y="4.5" width="18" height="16.5" rx="2" />
      <path d="M3 9.5h18M8 2.5v4M16 2.5v4" />
      <path d="M7.5 13.5h3M7.5 17h6" />
    </>
  ),
  pacientes: (
    <>
      <circle cx="9" cy="8" r="3.5" />
      <path d="M2.5 20a6.5 6.5 0 0 1 13 0" />
      <path d="M16 4.6a3.5 3.5 0 0 1 0 6.8M18 14.2a6.5 6.5 0 0 1 3.5 5.8" />
    </>
  ),
  espera: (
    <>
      <path d="M7 3h10M7 21h10" />
      <path d="M8 3v3.5a4 4 0 0 0 1.6 3.2L12 11.5l2.4-1.8A4 4 0 0 0 16 6.5V3M8 21v-3.5a4 4 0 0 1 1.6-3.2l2.4-1.8 2.4 1.8a4 4 0 0 1 1.6 3.2V21" />
    </>
  ),
  notificaciones: (
    <>
      <path d="M6 9a6 6 0 0 1 12 0c0 6 2.5 7.5 2.5 7.5h-17S6 15 6 9" />
      <path d="M10 20a2 2 0 0 0 4 0" />
    </>
  ),
  profesionales: (
    <>
      <path d="M6 3v5a4 4 0 0 0 8 0V3" />
      <path d="M10 12v2.5a5 5 0 0 0 10 0V12" />
      <circle cx="20" cy="10" r="2" />
    </>
  ),
  sillones: (
    <>
      <path d="M5 4v8h11l3 5" />
      <path d="M5 12v5h10" />
      <path d="M8 21h8M12 17v4" />
    </>
  ),
  prestaciones: (
    <path d="M7.5 3C5 3 3.5 5 3.5 7.5c0 3 1.5 4.5 2 7.5.4 2.6 1 6 2.5 6s1.6-3 2.2-5c.3-1 .8-1.5 1.8-1.5s1.5.5 1.8 1.5c.6 2 .7 5 2.2 5s2.1-3.4 2.5-6c.5-3 2-4.5 2-7.5C20.5 5 19 3 16.5 3 14.5 3 13.5 4 12 4S9.5 3 7.5 3z" />
  ),
  usuarios: (
    <>
      <path d="M12 3 4.5 6v5.5c0 4.5 3.2 8.2 7.5 9.5 4.3-1.3 7.5-5 7.5-9.5V6z" />
      <circle cx="12" cy="10" r="2.5" />
      <path d="M8 16.5a4.5 4.5 0 0 1 8 0" />
    </>
  ),
  configuracion: (
    <>
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z" />
    </>
  ),
  menu: <path d="M4 6h16M4 12h16M4 18h16" />,
  salir: <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" />,
  punto: <circle cx="12" cy="12" r="3" />,
  mail: (
    <>
      <rect x="3" y="5" width="18" height="14" rx="2" />
      <path d="m3 7 9 6 9-6" />
    </>
  ),
  mas: <path d="M12 5v14M5 12h14" />,
  buscar: (
    <>
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.5-3.5" />
    </>
  ),
  editar: <path d="M4 20h4L19 9l-4-4L4 16z" />,
  puntos: (
    <>
      <circle cx="5" cy="12" r="1.2" />
      <circle cx="12" cy="12" r="1.2" />
      <circle cx="19" cy="12" r="1.2" />
    </>
  ),
  izq: (
    <path d="m15 6-6 6 6 6" />
  ),
  der: (
    <path d="m9 6 6 6-6 6" />
  ),
  x: (
    <path d="M6 6l12 12M18 6 6 18" />
  ),
  checkCirculo: (
    <><circle cx="12" cy="12" r="9" /><path d="m8 12.5 2.8 2.8L16.5 9.5" /></>
  ),
  cancelado: (
    <><circle cx="12" cy="12" r="9" /><path d="m9 9 6 6M15 9l-6 6" /></>
  ),
  bloqueo: (
    <><circle cx="12" cy="12" r="9" /><path d="M5.6 5.6l12.8 12.8" /></>
  ),
  telefono: (
    <path d="M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2" />
  ),
  filtro: (
    <path d="M3 5h18l-7 8.5V20l-4-2v-4.5z" />
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
