// @vitest-environment node
import { existsSync, readdirSync, readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

// Vitest 1.x vacía los .css importados, así que se leen como texto (igual que estilosGlobales.test.ts).
const tokens = readFileSync(new URL("../../../../design-system/tokens.css", import.meta.url), "utf-8");
const CARPETAS = [new URL("./", import.meta.url), new URL("../../app/", import.meta.url)];

function hojasDeEstilo(): { nombre: string; css: string }[] {
  return CARPETAS.flatMap((carpeta) => {
    const ruta = fileURLToPath(carpeta);
    if (!existsSync(ruta)) return [];
    return readdirSync(ruta)
      .filter((archivo) => archivo.endsWith(".css"))
      .map((archivo) => ({ nombre: archivo, css: readFileSync(new URL(archivo, carpeta), "utf-8") }));
  });
}

const sinComentarios = (css: string) => css.replace(/\/\*[\s\S]*?\*\//g, "");

function variablesDeclaradas(css: string): Set<string> {
  return new Set([...css.matchAll(/(--[\w-]+)\s*:/g)].map((m) => m[1]));
}

function problemas(css: string, definidas: Set<string>): string[] {
  const limpio = sinComentarios(css);
  const propias = variablesDeclaradas(limpio);
  const encontrados: string[] = [];
  for (const [, variable] of limpio.matchAll(/var\((--[\w-]+)/g)) {
    if (!definidas.has(variable) && !propias.has(variable)) encontrados.push(`variable no definida: ${variable}`);
  }
  for (const [color] of limpio.matchAll(/#[0-9a-fA-F]{3,8}\b|\b(?:rgba?|hsla?)\(/g)) {
    encontrados.push(`color fijo: ${color}`);
  }
  return encontrados;
}

describe("Reglas visuales del badge de estado", () => {
  it("el texto del estado cancelado se tacha en la hoja del kit UI", () => {
    const ui = sinComentarios(readFileSync(new URL("./ui.css", import.meta.url), "utf-8"));
    expect(ui).toMatch(/\.st-cancelado\s+\.badge__txt\s*\{[^}]*text-decoration:\s*line-through/);
  });
});

describe("Estilos solo con tokens del design system", () => {
  it("El kit UI no usa colores fuera de tokens.css", () => {
    const definidas = variablesDeclaradas(tokens);
    const hojas = hojasDeEstilo();
    expect(hojas.map((h) => h.nombre)).toContain("ui.css");
    for (const { nombre, css } of hojas) {
      expect(problemas(css, definidas), nombre).toEqual([]);
    }

    // Triangulación: el detector informa colores fijos y variables inexistentes, e ignora comentarios.
    const css = ".x { color: #1B2733; background: rgb(0 0 0); border-color: var(--no-existe); } /* #fff */";
    expect(problemas(css, definidas)).toEqual([
      "variable no definida: --no-existe",
      "color fijo: #1B2733",
      "color fijo: rgb(",
    ]);
  });
});
