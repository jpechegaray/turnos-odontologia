import { describe, expect, it } from "vitest";
import { opcionesProfesional } from "./opciones";
import type { ProfesionalVinculable, UsuarioRespuesta } from "./tipos";

const libre: ProfesionalVinculable = { id: 1, nombre: "Ana", apellido: "Libre", activo: true };
const ocupado: ProfesionalVinculable = { id: 2, nombre: "Beto", apellido: "Ocupado", activo: true };
const inactivo: ProfesionalVinculable = { id: 3, nombre: "Ceci", apellido: "Inactiva", activo: false };

function odontologo(id: number, profesionalId: number): UsuarioRespuesta {
  return { id, email: `o${id}@consultorio.test`, nombre: "Odonto", rol: "ODONTOLOGO", profesionalId, activo: true };
}

describe("opcionesProfesional", () => {
  it("en el alta ofrece solo los activos sin usuario vinculado", () => {
    expect(opcionesProfesional([libre, ocupado, inactivo], [odontologo(7, 2)])).toEqual([libre]);
  });

  it("cuenta como ocupado el profesional de un usuario inactivo", () => {
    const desactivado = { ...odontologo(7, 2), activo: false };
    expect(opcionesProfesional([libre, ocupado], [desactivado])).toEqual([libre]);
  });

  it("en la edición incluye el profesional actual del usuario aunque esté inactivo", () => {
    const usuarios = [odontologo(7, 3), odontologo(8, 2)];
    expect(opcionesProfesional([libre, ocupado, inactivo], usuarios, 7)).toEqual([libre, inactivo]);
  });
});
