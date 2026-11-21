import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "../../design-system/tokens.css";
import "../../design-system/brand.js";
import "./index.css";
import App from "./App.tsx";

document.title = window.BRAND.name;

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
