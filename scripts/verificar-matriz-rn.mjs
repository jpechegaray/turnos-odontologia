// Verifica docs/matriz-rn-tests.md (C-51): las 38 reglas de la KB, ninguna fila sin test y cada escenario citado
// presente como nombre de test (@DisplayName o it/test) en el archivo indicado. Uso: node scripts/verificar-matriz-rn.mjs
import { existsSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const raiz = join(dirname(fileURLToPath(import.meta.url)), "..");
const leer = (ruta) => readFileSync(join(raiz, ruta), "utf8");

const reglas = [...new Set(leer("knowledge-base/05_reglas_de_negocio.md").match(/\*\*RN-\d{2} — /g))]
  .map((r) => r.slice(2, 7)).sort();
const filas = leer("docs/matriz-rn-tests.md").split("\n")
  .filter((linea) => /^\| RN-\d{2} \|/.test(linea))
  .map((linea) => linea.split("|").slice(1, -1).map((celda) => celda.trim()))
  .map(([rn, regla, capacidad, escenario, test]) => ({ rn, regla, capacidad, escenario, test: test.replace(/`/g, "") }));

const errores = [];
for (const rn of reglas) {
  if (!filas.some((f) => f.rn === rn)) errores.push(`${rn}: no tiene fila en la matriz`);
}
for (const f of filas) {
  if (!reglas.includes(f.rn)) errores.push(`${f.rn}: no existe en la base de conocimiento`);
  if (!f.escenario || !f.test) { errores.push(`${f.rn}: fila sin escenario o sin test`); continue; }
  if (!existsSync(join(raiz, f.test))) { errores.push(`${f.rn}: no existe el archivo ${f.test}`); continue; }
  const nombres = [...leer(f.test).matchAll(/(?:@DisplayName\(|\bit\(|\btest\()\s*(["'`])((?:\\.|(?!\1).)*)\1/g)]
    .map((m) => m[2].replace(/\\(["'`])/g, "$1"));
  if (!nombres.includes(f.escenario)) errores.push(`${f.rn}: "${f.escenario}" no es un test de ${f.test}`);
}

if (reglas.length !== 38) errores.push(`La base de conocimiento tiene ${reglas.length} reglas y se esperaban 38`);
if (errores.length > 0) {
  console.error(`La matriz RN→test tiene ${errores.length} problema(s):\n- ${errores.join("\n- ")}`);
  process.exit(1);
}
console.log(`Matriz RN→test verificada: ${reglas.length} reglas, ${filas.length} filas, todos los tests existen.`);
