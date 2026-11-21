import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

// brand.js necesita `window`: en los tests con `@vitest-environment node` no se carga.
if (typeof window !== "undefined") {
  await import("../../design-system/brand.js");
}

afterEach(cleanup);
