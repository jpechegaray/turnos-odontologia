// @vitest-environment node
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

// Vitest 1.x vacía los archivos .css (también con ?raw), así que se leen como texto con node:fs.
const leer = (ruta: string) => readFileSync(new URL(ruta, import.meta.url), "utf-8");
const tokens = leer("../../../design-system/tokens.css");
const logo = leer("../../../design-system/assets/logo.svg");
const brand = leer("../../../design-system/brand.js");
const main = leer("../main.tsx");

type Variables = Record<string, string>;

function bloque(css: string, selector: RegExp): Variables {
  const cuerpo = selector.exec(css)?.[1] ?? "";
  return Object.fromEntries([...cuerpo.matchAll(/(--[\w-]+)\s*:\s*([^;]+);/g)].map((m) => [m[1], m[2].trim()]));
}

const claro = bloque(tokens, /:root\s*\{([^}]*)\}/);
const oscuroMedia = bloque(tokens, /:root:not\(\[data-theme="light"\]\)\s*\{([^}]*)\}/);
const oscuroForzado = bloque(tokens, /:root\[data-theme="dark"\]\s*\{([^}]*)\}/);
const oscuro = { ...claro, ...oscuroMedia };

function luminancia(hex: string): number {
  const canales = hex.replace("#", "").match(/../g)!.map((c) => parseInt(c, 16) / 255);
  const [r, g, b] = canales.map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4));
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function contraste(a: string, b: string): number {
  const [l1, l2] = [luminancia(a), luminancia(b)].sort((x, y) => y - x);
  return (l1 + 0.05) / (l2 + 0.05);
}

const PARES: readonly (readonly [string, string])[] = [
  ["--color-on-primary", "--color-primary"],
  ["--color-text", "--color-bg"],
  ["--color-text", "--color-surface"],
  ["--color-text-muted", "--color-bg"],
  ["--color-text-muted", "--color-surface"],
  ["--color-link", "--color-surface"],
  ["--color-primary", "--color-primary-soft"],
  ["--color-info-text", "--color-info-soft"],
  ["--nav-fg", "--nav-bg"],
  ["--nav-fg-atenuado", "--nav-bg"],
  ["--nav-rotulo", "--nav-bg"],
  ["--nav-activo-fg", "--nav-activo-bg"],
  ["--nav-fg", "--nav-hover-bg"],
  ["--nav-contador-fg", "--nav-contador-bg"],
  ["--marca-acento", "--color-bg"],
  ["--nav-marca-acento", "--nav-bg"],
];

function paresBajoMinimo(variables: Variables): string[] {
  return PARES.filter(([fg, bg]) => !(contraste(variables[fg], variables[bg]) >= 4.5)).map(([fg, bg]) => `${fg} / ${bg}`);
}

function hojasDeEstilo(directorio: string): string[] {
  return readdirSync(directorio).flatMap((nombre) => {
    const ruta = join(directorio, nombre);
    if (statSync(ruta).isDirectory()) return hojasDeEstilo(ruta);
    return ruta.endsWith(".css") ? [ruta] : [];
  });
}

describe("Nombre del producto", () => {
  it("La pestaña y la marca muestran el nombre final", () => {
    expect(brand).toMatch(/var BRAND_NAME = 'OdontologIA';/);
    expect(brand.toLowerCase()).not.toContain("provisional");
    expect(main).toContain("document.title = window.BRAND.name");
  });
});

describe("Logo final accesible", () => {
  it("El logo no es el placeholder", () => {
    expect(logo).toMatch(/<title[^>]*>[^<]*OdontologIA[^<]*<\/title>/);
    expect(logo.toUpperCase()).not.toContain("PROVISIONAL");
    // El único http permitido es el espacio de nombres del SVG.
    expect(logo.replace(/xmlns="http:\/\/www\.w3\.org\/2000\/svg"/, "")).not.toMatch(/https?:\/\//);
  });
});

describe("Tipografía autoalojada", () => {
  it("La fuente se declara sin servidores externos", () => {
    expect(claro["--font-sans"]).toMatch(/^"Public Sans Variable",/);
    expect(claro["--font-sans"]).toMatch(/sans-serif$/);
    expect(main).toContain('import "@fontsource-variable/public-sans/wght.css";');

    const hojas = [...hojasDeEstilo(fileURLToPath(new URL("..", import.meta.url))), fileURLToPath(new URL("../../../design-system/tokens.css", import.meta.url))];
    const remotas = hojas.filter((ruta) => /@import\s+url\(\s*["']?https?:|fonts\.googleapis/.test(readFileSync(ruta, "utf-8")));
    expect(remotas).toEqual([]);
  });
});

describe("Paleta con contraste mínimo", () => {
  it("Los pares de texto principales superan 4.5:1", () => {
    expect(claro["--color-primary"]).toBe("#1B3A2F");
    expect(claro["--color-bg"]).toBe("#F5F1E8");
    expect(paresBajoMinimo(claro)).toEqual([]);
    expect(paresBajoMinimo(oscuro)).toEqual([]);

    // Triangulación: un par gris claro sobre blanco no llega a 4.5:1.
    expect(paresBajoMinimo({ ...claro, "--nav-fg": "#BBBBBB", "--nav-bg": "#FFFFFF" })).toContain("--nav-fg / --nav-bg");
  });

  it("Los dos bloques del modo oscuro son iguales", () => {
    expect(Object.keys(oscuroMedia).length).toBeGreaterThan(10);
    expect(oscuroForzado).toEqual(oscuroMedia);
  });
});
