import { describe, expect, it } from "vitest";
import { ErrorApi, errorDeRed, errorDesdeRespuesta } from "./errorApi";

function problema(estado: number, cuerpo: Record<string, unknown>): Response {
  return new Response(JSON.stringify({ status: estado, title: "t", ...cuerpo }), {
    status: estado,
    headers: { "Content-Type": "application/problem+json" },
  });
}

describe("Errores de la API tipados", () => {
  it("Un 409 se traduce a un error de conflicto con su código y recurso", async () => {
    const error = await errorDesdeRespuesta(
      problema(409, { detail: "El profesional ya tiene un turno en ese horario.", codigo: "RN-01", recurso: "PROFESIONAL" }),
    );
    expect(error).toBeInstanceOf(ErrorApi);
    expect(error.tipo).toBe("conflicto");
    expect(error.estado).toBe(409);
    expect(error.codigo).toBe("RN-01");
    expect(error.propiedades.recurso).toBe("PROFESIONAL");
    expect(error.detalle).toBe("El profesional ya tiene un turno en ese horario.");
    expect(error.message).toBe(error.detalle);
  });

  it("Un 422 se traduce a un error no procesable con el código de la regla", async () => {
    const error = await errorDesdeRespuesta(problema(422, { detail: "Duración inválida", codigo: "RN-09" }));
    expect(error.tipo).toBe("no-procesable");
    expect(error.codigo).toBe("RN-09");
  });

  it("Los errores 404 y 403 se traducen a sus tipos", async () => {
    expect((await errorDesdeRespuesta(problema(404, { detail: "No existe" }))).tipo).toBe("no-encontrado");
    expect((await errorDesdeRespuesta(problema(403, { detail: "Sin permiso" }))).tipo).toBe("prohibido");
  });

  it("Un 400 de validación conserva los campos inválidos", async () => {
    const error = await errorDesdeRespuesta(
      problema(400, { detail: "Invalid request content.", errores: [{ campo: "nombre", mensaje: "no debe estar vacío" }] }),
    );
    expect(error.tipo).toBe("validacion");
    expect(error.campos).toEqual([{ campo: "nombre", mensaje: "no debe estar vacío" }]);
    expect(error.codigo).toBeUndefined();
  });

  it("Un 401 se traduce a no autenticado", async () => {
    expect((await errorDesdeRespuesta(problema(401, { detail: "Se requiere autenticación" }))).tipo).toBe("no-autenticado");
  });

  it("Una respuesta de error sin Problem Details igual se tipa por su estado", async () => {
    const error = await errorDesdeRespuesta(
      new Response("<html>Bad Gateway</html>", { status: 502, headers: { "Content-Type": "text/html" } }),
    );
    expect(error.tipo).toBe("servidor");
    expect(error.estado).toBe(502);
    expect(error.detalle).not.toContain("html");
    expect(error.detalle.length).toBeGreaterThan(0);
  });

  it("otro 4xx sin tipo propio queda como error del cliente", async () => {
    expect((await errorDesdeRespuesta(problema(429, { detail: "Demasiadas solicitudes" }))).tipo).toBe("cliente");
  });

  it("Un fallo de red se informa como error de red", () => {
    const error = errorDeRed(new TypeError("Failed to fetch"));
    expect(error.tipo).toBe("red");
    expect(error.estado).toBe(0);
    expect(error.detalle).not.toContain("Failed to fetch");
  });
});
