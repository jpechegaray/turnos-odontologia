/**
 * Diente con destello de la marca, en línea para que tome el color del contexto (`currentColor`) y el
 * destello de `--marca-destello`. Mismos trazos que design-system/assets/logo.svg. Siempre decorativo:
 * el nombre de la marca va escrito al lado.
 */
export function Logo() {
  return (
    <svg className="logo" viewBox="0 0 48 48" aria-hidden="true" focusable="false">
      <path
        className="logo__diente"
        d="M14 4C8 4 4 9 4 15.5c0 6.5 3 10.5 4.2 16.5C9.4 38 10.6 44 14 44c3.3 0 4.2-4.6 5-9.4.6-3.6 2.4-5.6 5-5.6s4.4 2 5 5.6c.8 4.8 1.7 9.4 5 9.4 3.4 0 4.6-6 5.8-12C41 26 44 22 44 15.5 44 9 40 4 34 4c-4 0-6.2 2.2-10 2.2S18 4 14 4z"
      />
      <path className="logo__destello" d="M33 10.5c.5 3.2 1.8 4.5 5 5-3.2.5-4.5 1.8-5 5-.5-3.2-1.8-4.5-5-5 3.2-.5 4.5-1.8 5-5z" />
    </svg>
  );
}

/** Nombre de la marca con "IA" resaltado. El texto accesible es el nombre completo de BRAND. */
export function NombreMarca() {
  const nombre = window.BRAND.name;
  const corte = nombre.endsWith("IA") ? nombre.length - 2 : nombre.length;
  return (
    <>
      {nombre.slice(0, corte)}
      {corte < nombre.length ? <span className="marca__acento">{nombre.slice(corte)}</span> : null}
    </>
  );
}
