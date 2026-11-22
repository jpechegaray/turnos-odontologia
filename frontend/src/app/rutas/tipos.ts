import type { ComponentType } from "react";

/** Roles del personal (RN-33). */
export type Rol = "ODONTOLOGO" | "RECEPCION" | "ADMIN";

export type EntradaMenu = Readonly<{
  etiqueta: string;
  /** Menor primero; a igual orden, alfabético por etiqueta. */
  orden: number;
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
