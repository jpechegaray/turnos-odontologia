import { useEffect, useState } from "react";

const RETARDO_MAXIMO = 2_147_483_647;

/** Actualiza el reloj cuando se alcanza el próximo límite relevante de la interfaz. */
export function useAhoraEnLimites(limites: readonly number[]): number {
  const [ahora, cambiarAhora] = useState(Date.now);
  const siguiente = limites.reduce((cercano, limite) => limite > ahora ? Math.min(cercano, limite) : cercano, Infinity);
  useEffect(() => {
    if (!Number.isFinite(siguiente)) return;
    const demora = Math.max(1, Math.min(siguiente - Date.now(), RETARDO_MAXIMO));
    const temporizador = window.setTimeout(() => cambiarAhora(Date.now()), demora);
    return () => window.clearTimeout(temporizador);
  }, [ahora, siguiente]);
  return ahora;
}
