/**
 * Error de la API traducido desde Problem Details (RFC 7807). Las pantallas deciden qué mostrar por
 * `tipo` y `codigo` (la regla RN-XX) sin mirar estados HTTP.
 */
export type TipoErrorApi =
  | "validacion"
  | "no-autenticado"
  | "prohibido"
  | "no-encontrado"
  | "conflicto"
  | "no-procesable"
  | "cliente"
  | "servidor"
  | "red";

export type CampoInvalido = Readonly<{ campo: string; mensaje: string }>;

const MENSAJE_GENERICO = "No pudimos completar la operación. Intentá de nuevo en unos minutos.";
const MENSAJE_RED = "No pudimos conectarnos con el servidor. Revisá tu conexión e intentá de nuevo.";

const TIPOS_POR_ESTADO: Readonly<Record<number, TipoErrorApi>> = {
  400: "validacion",
  401: "no-autenticado",
  403: "prohibido",
  404: "no-encontrado",
  409: "conflicto",
  422: "no-procesable",
};

/** Campos estándar de Problem Details que no van a `propiedades`. */
const CAMPOS_ESTANDAR = new Set(["type", "title", "status", "detail", "instance", "codigo", "errores"]);

export class ErrorApi extends Error {
  readonly tipo: TipoErrorApi;
  readonly estado: number;
  readonly detalle: string;
  readonly codigo?: string;
  readonly campos: readonly CampoInvalido[];
  /** Propiedades de contexto adicionales del error, por ejemplo `recurso`. */
  readonly propiedades: Readonly<Record<string, unknown>>;

  constructor(datos: {
    tipo: TipoErrorApi;
    estado: number;
    detalle: string;
    codigo?: string;
    campos?: readonly CampoInvalido[];
    propiedades?: Record<string, unknown>;
  }) {
    super(datos.detalle);
    this.name = "ErrorApi";
    this.tipo = datos.tipo;
    this.estado = datos.estado;
    this.detalle = datos.detalle;
    this.codigo = datos.codigo;
    this.campos = datos.campos ?? [];
    this.propiedades = datos.propiedades ?? {};
  }
}

function tipoPorEstado(estado: number): TipoErrorApi {
  if (estado >= 500) return "servidor";
  return TIPOS_POR_ESTADO[estado] ?? "cliente";
}

function esObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === "object" && valor !== null && !Array.isArray(valor);
}

function camposInvalidos(valor: unknown): CampoInvalido[] {
  if (!Array.isArray(valor)) return [];
  return valor
    .filter(esObjeto)
    .filter((e) => typeof e.campo === "string")
    .map((e) => ({ campo: String(e.campo), mensaje: typeof e.mensaje === "string" ? e.mensaje : "" }));
}

async function leerProblema(respuesta: Response): Promise<Record<string, unknown> | null> {
  const tipoContenido = respuesta.headers.get("Content-Type") ?? "";
  if (!tipoContenido.includes("json")) return null;
  try {
    const cuerpo: unknown = await respuesta.json();
    return esObjeto(cuerpo) ? cuerpo : null;
  } catch {
    return null;
  }
}

export async function errorDesdeRespuesta(respuesta: Response): Promise<ErrorApi> {
  const problema = await leerProblema(respuesta);
  const tipo = tipoPorEstado(respuesta.status);
  if (!problema) {
    return new ErrorApi({ tipo, estado: respuesta.status, detalle: MENSAJE_GENERICO });
  }
  const propiedades = Object.fromEntries(Object.entries(problema).filter(([clave]) => !CAMPOS_ESTANDAR.has(clave)));
  return new ErrorApi({
    tipo,
    estado: respuesta.status,
    detalle: typeof problema.detail === "string" && problema.detail ? problema.detail : MENSAJE_GENERICO,
    codigo: typeof problema.codigo === "string" ? problema.codigo : undefined,
    campos: camposInvalidos(problema.errores),
    propiedades,
  });
}

/** La causa técnica no se expone en el mensaje. */
export function errorDeRed(causa: unknown): ErrorApi {
  const error = new ErrorApi({ tipo: "red", estado: 0, detalle: MENSAJE_RED });
  return Object.assign(error, { cause: causa });
}
