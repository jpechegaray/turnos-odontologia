// @vitest-environment node
import { readFileSync } from "node:fs";
import { expect, it } from "vitest";

// index.html es el documento de toda la SPA: la política vale para cada pantalla y en cualquier servidor estático.
const indice = readFileSync(new URL("../../../index.html", import.meta.url), "utf-8");

it("La SPA declara la política de referencia sin referer", () => {
  expect(indice).toMatch(/<meta\s+name="referrer"\s+content="no-referrer"\s*\/?>/u);
});
