import { vi } from "vitest";

export function simularSesionActiva() {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(
    Response.json({ id: 1, email: "personal@example.test", rol: "ADMIN" }),
  ));
}
