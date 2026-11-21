// @vitest-environment node
import { ESLint } from "eslint";
import { describe, expect, it } from "vitest";

const eslint = new ESLint({ cwd: process.cwd() });

async function analizar(codigo: string) {
  const [resultado] = await eslint.lintText(codigo, { filePath: "src/fixtureAny.ts" });
  return resultado.messages;
}

describe("Chequeo estático", () => {
  it("El lint rechaza el uso explícito de any", async () => {
    const mensajes = await analizar("export const valor: any = 1;\n");
    expect(mensajes).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ ruleId: "@typescript-eslint/no-explicit-any", severity: 2 }),
      ]),
    );
    // Triangulación: un tipo explícito distinto de any no produce ese error.
    const sinAny = await analizar("export const valor: number = 1;\n");
    expect(sinAny.filter((mensaje) => mensaje.ruleId === "@typescript-eslint/no-explicit-any")).toEqual([]);
  });
});
