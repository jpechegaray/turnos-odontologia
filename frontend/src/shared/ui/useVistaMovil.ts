import { useSyncExternalStore } from "react";

const consulta = "(max-width: 900px)";

function suscribir(notificar: () => void) {
  const vista = window.matchMedia?.(consulta);
  vista?.addEventListener("change", notificar);
  return () => vista?.removeEventListener("change", notificar);
}

export function useVistaMovil() {
  return useSyncExternalStore(suscribir, () => window.matchMedia?.(consulta).matches ?? false, () => false);
}
