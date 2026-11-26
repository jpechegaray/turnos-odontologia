import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "../../shared/api/clienteApi";
import type { PanelProfesionalProps } from "./profesionales/PaginaProfesionales";

export type EstadoListado<T> =
  | Readonly<{ tipo: "cargando" }>
  | Readonly<{ tipo: "error" }>
  | Readonly<{ tipo: "listo"; elementos: readonly T[] }>;

/** Qué hace el panel después de mostrar el mensaje de un error de escritura. */
export type ReaccionError =
  /** Volver a consultar el listado. */
  | "recargar"
  /** El profesional ya no existe: la página muestra el mensaje y refresca su lista. */
  | "refrescarLista"
  /** Solo mostrar el mensaje (el formulario conserva lo ingresado). */
  | "ninguna";

export type ErrorTraducido = Readonly<{ mensaje: string; reaccion: ReaccionError }>;

type Opciones = Readonly<{
  /** Ruta de la API relativa a `/api`, p. ej. `/profesionales/1/horarios`. */
  ruta: string;
  traducirError: (error: unknown) => ErrorTraducido;
}> &
  Pick<PanelProfesionalProps, "refrescarLista" | "informarError">;

/**
 * Listado de un recurso del profesional con escrituras que recargan al terminar. Un contador descarta
 * las respuestas viejas y una bandera evita escrituras simultáneas. Lo usan los paneles de horarios
 * y bloqueos (C-22).
 */
export function useListadoPanel<T>({ ruta, traducirError, refrescarLista, informarError }: Opciones) {
  const [estado, actualizarEstado] = useState<EstadoListado<T>>({ tipo: "cargando" });
  const [guardando, actualizarGuardando] = useState(false);
  const [aviso, actualizarAviso] = useState<string>();
  const [error, actualizarError] = useState<string>();
  /** Cambia tras cada escritura exitosa; sirve de `key` para vaciar el formulario de alta. */
  const [version, actualizarVersion] = useState(0);
  const solicitud = useRef(0);
  const pendiente = useRef(false);

  /** `silenciosa` recarga sin pasar por «Cargando…», para no desmontar la lista tras escribir. */
  const cargar = useCallback(
    async (silenciosa = false) => {
      const actual = ++solicitud.current;
      if (!silenciosa) actualizarEstado({ tipo: "cargando" });
      try {
        const elementos = await api.get<T[]>(ruta);
        if (actual === solicitud.current) {
          actualizarEstado(Array.isArray(elementos) ? { tipo: "listo", elementos } : { tipo: "error" });
        }
      } catch {
        if (actual === solicitud.current) actualizarEstado({ tipo: "error" });
      }
    },
    [ruta],
  );

  useEffect(() => {
    void cargar();
  }, [cargar]);

  const limpiarMensajes = useCallback(() => {
    actualizarAviso(undefined);
    actualizarError(undefined);
  }, []);

  /**
   * Ejecuta una escritura, anuncia `exito` y recarga. Ante un error lo traduce, aplica su reacción y
   * lo vuelve a lanzar para que el formulario conserve lo ingresado.
   */
  async function escribir(operacion: () => Promise<unknown>, exito: string): Promise<void> {
    if (pendiente.current) return;
    pendiente.current = true;
    actualizarGuardando(true);
    limpiarMensajes();
    try {
      await operacion();
      actualizarVersion((v) => v + 1);
      actualizarAviso(exito);
      await cargar(true);
    } catch (causa) {
      const { mensaje, reaccion } = traducirError(causa);
      if (reaccion === "refrescarLista") {
        // Si el profesional ya no está, la recarga de la página desmonta este panel.
        informarError(mensaje);
        void refrescarLista();
      } else {
        actualizarError(mensaje);
        if (reaccion === "recargar") void cargar(true);
      }
      throw causa;
    } finally {
      pendiente.current = false;
      actualizarGuardando(false);
    }
  }

  return { estado, cargar, escribir, guardando, aviso, error, version, limpiarMensajes };
}
