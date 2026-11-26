import type { ComponentType } from "react";
import type { NombreIcono } from "../../shared/ui/Icono";

/** Roles del personal (RN-33). */
export type Rol = "ODONTOLOGO" | "RECEPCION" | "ADMIN";

/** Secciones del menú lateral, en el orden en que se muestran. */
export const GRUPOS_MENU = [
  { id: "atencion", titulo: "Atención" },
  { id: "consultorio", titulo: "Consultorio" },
  { id: "administracion", titulo: "Administración" },
] as const;

export type GrupoMenu = (typeof GRUPOS_MENU)[number]["id"];

export type EntradaMenu = Readonly<{
  etiqueta: string;
  /** Menor primero; a igual orden, alfabético por etiqueta. */
  orden: number;
  /** Sin grupo, la entrada va en Atención. */
  grupo?: GrupoMenu;
  /** Ícono decorativo de la entrada; sin ícono se usa uno genérico. */
  icono?: NombreIcono;
}>;

/**
 * Lo que declara cada `features/<feature>/routes.ts`. El componente puede ser `lazy(() => import(...))`
 * para dividir el bundle.
 */
export type RutaFeature = Readonly<{
  ruta: string;
  componente: ComponentType;
  /** Sin entrada de menú la ruta existe pero no aparece en la navegación. */
  menu?: EntradaMenu;
  /** Por defecto es una pantalla interna del personal. */
  ambito?: "publico" | "ingreso";
  /** Roles permitidos en la pantalla interna. La API conserva la autorización efectiva. */
  roles?: readonly Rol[];
}>;

/** Forma del módulo `routes.ts` de una feature. */
export type ModuloRutas = Readonly<{ rutas: readonly RutaFeature[] }>;

export type EntradaMenuResuelta = EntradaMenu & Readonly<{ ruta: string }>;

export type RegistroRutas = Readonly<{
  rutas: readonly RutaFeature[];
  menu: readonly EntradaMenuResuelta[];
}>;
