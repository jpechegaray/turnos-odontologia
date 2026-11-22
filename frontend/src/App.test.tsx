import { render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import App from "./App";
import { simularSesionActiva } from "./app/sesion.fixture";

describe("App", () => {
  const marcaOriginal = window.BRAND;

  beforeEach(simularSesionActiva);
  afterEach(() => {
    vi.unstubAllGlobals();
    window.BRAND = marcaOriginal;
  });

  it("La App muestra el nombre de la marca tomado de BRAND", async () => {
    render(<App />);
    expect(await screen.findByRole("heading", { name: window.BRAND.name })).toBeInTheDocument();
  });

  it("La App refleja un cambio del nombre en BRAND", async () => {
    const nombreProvisional = window.BRAND.name;
    const nombreFicticio = "Consultorio Ficticio de Prueba";
    window.BRAND = { name: nombreFicticio, logoAlt: `Logo de ${nombreFicticio}` };

    render(<App />);

    expect(await screen.findByRole("heading", { name: nombreFicticio })).toBeInTheDocument();
    expect(screen.queryByText(nombreProvisional)).not.toBeInTheDocument();
  });
});
