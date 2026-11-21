// @vitest-environment node
import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";

// Vitest 1.x vacía los archivos .css (también con ?raw), así que se leen como texto con node:fs.
const estilosGlobales = readFileSync(new URL("../index.css", import.meta.url), "utf-8");
const tokens = readFileSync(new URL("../../../design-system/tokens.css", import.meta.url), "utf-8");

function declaracionesDeBody(css: string): string {
  return /body\s*\{([^}]*)\}/.exec(css)?.[1] ?? "";
}

function usaVariable(declaraciones: string, propiedad: string): boolean {
  return new RegExp(String.raw`(^|[;\s])${propiedad}\s*:\s*var\(--[\w-]+\)`).test(declaraciones);
}

function variablesUsadas(css: string): string[] {
  return [...css.matchAll(/var\((--[\w-]+)\)/g)].map((coincidencia) => coincidencia[1]);
}

function variablesDeclaradas(css: string): Set<string> {
  return new Set([...css.matchAll(/(--[\w-]+)\s*:/g)].map((coincidencia) => coincidencia[1]));
}

function problemas(css: string, definidas: Set<string>): string[] {
  const encontrados: string[] = [];
  const body = declaracionesDeBody(css);
  for (const propiedad of ["background", "color", "font-family"]) {
    if (!usaVariable(body, propiedad)) encontrados.push(`body: ${propiedad} sin variable`);
  }
  for (const variable of variablesUsadas(css)) {
    if (!definidas.has(variable)) encontrados.push(`variable no definida: ${variable}`);
  }
  if (/#[0-9a-fA-F]{3,8}\b|\brgba?\(|\bhsla?\(/.test(css)) encontrados.push("color fijo");
  return encontrados;
}

describe("Estilos globales", () => {
  it("Los estilos globales usan solo variables definidas en tokens.css", () => {
    const definidas = variablesDeclaradas(tokens);
    expect(definidas.has("--color-bg")).toBe(true);
    expect(problemas(estilosGlobales, definidas)).toEqual([]);

    // Triangulación: el detector informa un texto con color fijo y una variable inexistente.
    const css = "body { background: var(--color-bg); color: #123456; font-family: var(--no-existe); }";
    expect(problemas(css, definidas)).toEqual(
      expect.arrayContaining(["body: color sin variable", "variable no definida: --no-existe", "color fijo"]),
    );
  });
});
