import "./ui.css";

export type OpcionSegmentada<T extends string> = Readonly<{ valor: T; etiqueta: string }>;

type Props<T extends string> = Readonly<{
  titulo: string;
  opciones: readonly OpcionSegmentada<T>[];
  valor: T;
  onElegir: (valor: T) => void;
  deshabilitado?: boolean;
}>;

/** Grupo de botones excluyentes; el elegido lleva `aria-pressed="true"`. */
export function Segmentado<T extends string>({ titulo, opciones, valor, onElegir, deshabilitado }: Props<T>) {
  return (
    <div className="segmentado" role="group" aria-label={titulo}>
      {opciones.map((opcion) => (
        <button
          key={opcion.valor}
          type="button"
          aria-pressed={opcion.valor === valor}
          disabled={deshabilitado}
          onClick={() => onElegir(opcion.valor)}
        >
          {opcion.etiqueta}
        </button>
      ))}
    </div>
  );
}
