import { describe, expect, it } from "vitest";
import { MENSAJE_PROFESIONAL_OBLIGATORIO, passwordValida, validarUsuario } from "./validacion";

describe("passwordValida", () => {
  it("acepta exactamente 8 caracteres", () => {
    expect(passwordValida("12345678")).toBe(true);
  });

  it("rechaza 7 caracteres", () => {
    expect(passwordValida("1234567")).toBe(false);
  });

  it("acepta exactamente 72 bytes", () => {
    expect(passwordValida("a".repeat(72))).toBe(true);
    expect(passwordValida("ñ".repeat(36))).toBe(true);
  });

  it("rechaza 73 bytes aunque tenga pocos caracteres", () => {
    expect(passwordValida("a".repeat(71) + "ñ")).toBe(false);
  });

  it("cuenta code points y no unidades UTF-16", () => {
    // 4 emojis son 8 unidades UTF-16 pero solo 4 caracteres: no alcanza el mínimo.
    expect(passwordValida("😀😀😀😀")).toBe(false);
  });
});

describe("validarUsuario", () => {
  const base = { email: "ok@consultorio.test", nombre: "Nombre", profesionalId: null } as const;

  it("exige profesional solo al odontólogo", () => {
    expect(validarUsuario({ ...base, rol: "ODONTOLOGO" }).profesionalId).toBe(MENSAJE_PROFESIONAL_OBLIGATORIO);
    expect(validarUsuario({ ...base, rol: "RECEPCION" }).profesionalId).toBeUndefined();
    expect(validarUsuario({ ...base, rol: "ODONTOLOGO", profesionalId: 10 }).profesionalId).toBeUndefined();
  });

  it("no valida la contraseña cuando no se envía (edición)", () => {
    expect(validarUsuario({ ...base, rol: "ADMIN" }).password).toBeUndefined();
  });

  it("rechaza un email de más de 254 caracteres", () => {
    const largo = `${"a".repeat(250)}@x.co`;
    expect(validarUsuario({ ...base, rol: "ADMIN", email: largo }).email).toBeDefined();
  });
});
