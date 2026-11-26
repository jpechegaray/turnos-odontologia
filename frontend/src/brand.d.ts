// Tipo de la marca que define design-system/brand.js (window.BRAND).
export {};

declare global {
  interface Window {
    BRAND: Readonly<{ name: string; subtitulo: string; logoAlt: string }>;
  }
}
