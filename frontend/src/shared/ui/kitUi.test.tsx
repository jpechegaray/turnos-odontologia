import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { Alerta, BadgeEstadoTurno, Boton, Campo, Tabla, Tarjeta, Toast, type EstadoVisualTurno } from "./index";

describe("Botón", () => {
  it("El botón ejecuta su acción y respeta la variante", async () => {
    const accion = vi.fn();
    render(<Boton variante="peligro" onClick={accion}>Cancelar turno</Boton>);

    const boton = screen.getByRole("button", { name: "Cancelar turno" });
    await userEvent.click(boton);

    expect(accion).toHaveBeenCalledTimes(1);
    expect(boton).toHaveClass("btn", "btn--danger");
    expect(boton).toHaveAttribute("type", "button");
  });

  it("Un botón deshabilitado no ejecuta su acción", async () => {
    const accion = vi.fn();
    render(<Boton disabled onClick={accion}>Guardar</Boton>);

    await userEvent.click(screen.getByRole("button", { name: "Guardar" }));

    expect(accion).not.toHaveBeenCalled();
  });

  it("aplica primaria por defecto, tamaño chico, ancho completo y permite type submit", () => {
    render(<Boton tamano="chico" anchoCompleto type="submit">Enviar</Boton>);
    const boton = screen.getByRole("button", { name: "Enviar" });
    expect(boton).toHaveClass("btn--primary", "btn--sm", "btn--block");
    expect(boton).toHaveAttribute("type", "submit");
  });
});

describe("Campo de formulario accesible", () => {
  it("El campo asocia la etiqueta y la ayuda al control", () => {
    render(<Campo etiqueta="Nombre" ayuda="Como figura en el catálogo" />);

    const control = screen.getByLabelText("Nombre");
    expect(control).toHaveAccessibleDescription("Como figura en el catálogo");
    expect(control).not.toHaveAttribute("aria-invalid");
  });

  it("Un campo con error queda marcado como inválido", () => {
    render(<Campo etiqueta="Duración" ayuda="En minutos" error="La duración debe ser múltiplo de 5" />);

    const control = screen.getByLabelText("Duración");
    expect(control).toHaveAttribute("aria-invalid", "true");
    expect(control).toHaveAccessibleDescription(expect.stringContaining("La duración debe ser múltiplo de 5"));
    expect(control).toHaveAccessibleDescription(expect.stringContaining("En minutos"));
  });
});

describe("Badge de estado de turno", () => {
  const casos: [EstadoVisualTurno, string, string][] = [
    ["PROGRAMADO", "Reservado", "st-reservado"],
    ["CONFIRMADO", "Confirmado", "st-confirmado"],
    ["ATENDIDO", "Atendido", "st-atendido"],
    ["AUSENTE", "Ausente", "st-ausente"],
    ["CANCELADO", "Cancelado", "st-cancelado"],
    ["CANCELADO_TARDE", "Cancelación tardía", "st-tardia"],
    ["BLOQUEO", "Bloqueo", "st-bloqueo"],
  ];

  it("Cada estado de turno se muestra con su texto y su estilo", () => {
    for (const [estado, texto, clase] of casos) {
      const { container, unmount } = render(<BadgeEstadoTurno estado={estado} />);
      const badge = container.querySelector(".badge");
      expect(badge, estado).toHaveClass(clase);
      expect(badge).toHaveTextContent(texto);
      expect(badge?.querySelector("svg")).toHaveAttribute("aria-hidden", "true");
      unmount();
    }
  });

  it("El estado cancelado se muestra tachado", () => {
    render(<BadgeEstadoTurno estado="CANCELADO" />);
    expect(screen.getByText("Cancelado")).toHaveClass("badge__txt");
    expect(screen.getByText("Cancelado").closest(".badge")).toHaveClass("st-cancelado");
  });
});

describe("Tabla", () => {
  type Fila = { id: number; nombre: string; duracion: number };
  const columnas = [
    { clave: "nombre", titulo: "Nombre", celda: (f: Fila) => f.nombre },
    { clave: "duracion", titulo: "Duración", numerica: true, celda: (f: Fila) => `${f.duracion} min` },
  ];

  it("La tabla muestra título, encabezados y filas", () => {
    const filas: Fila[] = [
      { id: 1, nombre: "Consulta", duracion: 30 },
      { id: 2, nombre: "Limpieza", duracion: 45 },
    ];
    render(<Tabla titulo="Prestaciones" columnas={columnas} filas={filas} claveFila={(f) => f.id} mensajeVacio="No hay prestaciones" />);

    const tabla = screen.getByRole("table", { name: "Prestaciones" });
    expect(within(tabla).getByRole("columnheader", { name: "Nombre" })).toBeInTheDocument();
    expect(within(tabla).getByRole("columnheader", { name: "Duración" })).toHaveClass("num");
    expect(within(tabla).getAllByRole("row")).toHaveLength(3);
    expect(within(tabla).getByText("45 min")).toHaveClass("num");
  });

  it("Una tabla sin filas muestra el mensaje de vacío", () => {
    render(<Tabla titulo="Prestaciones" columnas={columnas} filas={[]} claveFila={(f: Fila) => f.id} mensajeVacio="No hay prestaciones" />);
    expect(screen.getByText("No hay prestaciones")).toBeInTheDocument();
  });
});

describe("Toast, alerta y tarjeta", () => {
  it("El toast se anuncia y ejecuta su acción", async () => {
    const deshacer = vi.fn();
    render(<Toast mensaje="Turno cancelado" accion={{ etiqueta: "Deshacer", alHacerClic: deshacer }} />);

    expect(screen.getByRole("status")).toHaveTextContent("Turno cancelado");
    await userEvent.click(screen.getByRole("button", { name: "Deshacer" }));
    expect(deshacer).toHaveBeenCalledTimes(1);
  });

  it("Una alerta de peligro se anuncia de inmediato", () => {
    render(<Alerta tipo="peligro" titulo="No se pudo guardar">El horario ya está ocupado.</Alerta>);

    const alerta = screen.getByRole("alert");
    expect(alerta).toHaveTextContent("No se pudo guardar");
    expect(alerta).toHaveTextContent("El horario ya está ocupado.");
  });

  it("una alerta informativa no interrumpe al lector de pantalla", () => {
    render(<Alerta tipo="info" titulo="Recordatorio">Se envía 24 h antes.</Alerta>);
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.getByText("Recordatorio")).toBeInTheDocument();
  });

  it("La tarjeta muestra su título y contenido", () => {
    render(<Tarjeta titulo="Próximo turno"><p>Hoy 09:00</p></Tarjeta>);
    expect(screen.getByRole("heading", { name: "Próximo turno" })).toBeInTheDocument();
    expect(screen.getByText("Hoy 09:00")).toBeInTheDocument();
  });
});
