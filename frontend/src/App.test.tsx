import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import App from "./App";

describe("App", () => {
  const marcaOriginal = window.BRAND;

  afterEach(() => {
    window.BRAND = marcaOriginal;
  });

  it("La App muestra el nombre de la marca tomado de BRAND", () => {
    render(<App />);
    expect(screen.getByRole("heading", { name: window.BRAND.name })).toBeInTheDocument();
  });

  it("La App refleja un cambio del nombre en BRAND", () => {
    const nombreProvisional = window.BRAND.name;
    const nombreFicticio = "Consultorio Ficticio de Prueba";
    window.BRAND = { name: nombreFicticio, logoAlt: `Logo de ${nombreFicticio}` };

    render(<App />);

    expect(screen.getByRole("heading", { name: nombreFicticio })).toBeInTheDocument();
    expect(screen.queryByText(nombreProvisional)).not.toBeInTheDocument();
  });
});
