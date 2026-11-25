// @vitest-environment node
import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";

// Vitest vacía los .css importados, así que se leen como texto (igual que estilosKitUi.test.ts).
const tokens = readFileSync(new URL("../../../../design-system/tokens.css", import.meta.url), "utf-8");
const agenda = readFileSync(new URL("./agenda.css", import.meta.url), "utf-8");

const sinComentarios = (css: string) => css.replace(/\/\*[\s\S]*?\*\//g, "");
const declaradas = (css: string) => new Set([...css.matchAll(/(--[\w-]+)\s*:/g)].map((m) => m[1]));

describe("Estilos de la agenda", () => {
  it("La hoja de la agenda usa solo colores y variables de tokens.css", () => {
    const limpio = sinComentarios(agenda);
    const definidas = declaradas(tokens);
    // Las que llegan en línea desde los componentes.
    const enLinea = new Set(["--agenda-columnas", "--agenda-filas"]);
    const indefinidas = [...limpio.matchAll(/var\((--[\w-]+)/g)]
      .map(([, variable]) => variable)
      .filter((variable) => !definidas.has(variable) && !enLinea.has(variable));
    const colores = [...limpio.matchAll(/#[0-9a-fA-F]{3,8}\b|\b(?:rgba?|hsla?)\(/g)].map(([color]) => color);

    expect(indefinidas).toEqual([]);
    expect(colores).toEqual([]);
  });

  it("Un turno cancelado se ve tachado en la grilla y en la semana", () => {
    const limpio = sinComentarios(agenda);
    expect(limpio).toMatch(/\.slot--cancelado strong[^{]*\{[^}]*text-decoration:\s*line-through/);
    expect(limpio).toMatch(/\.semana__item--cancelado strong[^{]*\{[^}]*text-decoration:\s*line-through/);
  });
});
