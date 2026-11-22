import { construirRegistro } from "./registro";
import type { ModuloRutas } from "./tipos";

/**
 * Único punto donde se descubren las features. Para sumar pantallas, una feature crea su propio
 * `features/<feature>/routes.ts`; este archivo no se edita.
 */
const modulos = import.meta.glob<ModuloRutas>("../../features/*/routes.ts", { eager: true });

export const registroDeFeatures = construirRegistro(modulos);
