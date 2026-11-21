import { BrowserRouter } from "react-router-dom";
import { AppRutas } from "./app/AppRutas";
import { registroDeFeatures } from "./app/rutas/modulos";

function App() {
  return (
    <BrowserRouter>
      <AppRutas registro={registroDeFeatures} />
    </BrowserRouter>
  );
}

export default App;
