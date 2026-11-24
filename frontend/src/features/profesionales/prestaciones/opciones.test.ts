import { expect, it } from "vitest";
import { construirOpciones, idsSeleccionados, seleccionInicial } from "./opciones";

const CONSULTA = { id: 10, nombre: "Consulta", duracionDefaultMin: 30, activa: true };
const LIMPIEZA = { id: 11, nombre: "Limpieza", duracionDefaultMin: 45, activa: true };
const BLANQUEAMIENTO = { id: 12, nombre: "Blanqueamiento", duracionDefaultMin: 60, activa: false };
const ORTODONCIA = { id: 13, nombre: "Ortodoncia", duracionDefaultMin: 90, activa: false };

it("construirOpciones y seleccionInicial: une asociadas y activas del catálogo, ordenadas por nombre", () => {
  const asociadas = [BLANQUEAMIENTO, CONSULTA];

  const opciones = construirOpciones([LIMPIEZA, ORTODONCIA, CONSULTA, BLANQUEAMIENTO], asociadas);

  expect(opciones.map((p) => p.nombre)).toEqual(["Blanqueamiento", "Consulta", "Limpieza"]);
  expect(opciones.find((p) => p.id === BLANQUEAMIENTO.id)?.activa).toBe(false);
  expect([...seleccionInicial(asociadas)].sort()).toEqual([CONSULTA.id, BLANQUEAMIENTO.id].sort());
  // Una asociada que ya no figura en el catálogo se sigue ofreciendo para poder quitarla.
  expect(construirOpciones([LIMPIEZA], [BLANQUEAMIENTO]).map((p) => p.id)).toEqual([BLANQUEAMIENTO.id, LIMPIEZA.id]);
});

it("idsSeleccionados: devuelve los ids elegidos que siguen siendo opción, en orden", () => {
  const opciones = construirOpciones([CONSULTA, LIMPIEZA], [CONSULTA]);

  expect(idsSeleccionados(opciones, new Set([LIMPIEZA.id, CONSULTA.id]))).toEqual([CONSULTA.id, LIMPIEZA.id]);
  // Un id seleccionado que ya no es opción (p. ej. dado de baja sin estar asociado) no se envía.
  expect(idsSeleccionados(opciones, new Set([ORTODONCIA.id]))).toEqual([]);
});
