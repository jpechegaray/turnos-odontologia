import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

const URL_BACKEND = "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  envDir: "..",
  server: {
    proxy: {
      "/api": { target: URL_BACKEND },
    },
    fs: { allow: [".", "../design-system"] },
  },
  test: {
    environment: "jsdom",
    globals: false,
    setupFiles: ["./src/setupTests.ts"],
  },
});
