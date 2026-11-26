import { useEffect, useRef } from "react";

/**
 * Pantalla única para un token inexistente, vencido o revocado (RN-25, RN-38): no distingue el motivo ni repite
 * el token, para no dar pistas a quien prueba enlaces.
 */
export function EnlaceNoDisponible() {
  const titulo = useRef<HTMLHeadingElement>(null);
  useEffect(() => { titulo.current?.focus(); }, []);
  return (
    <div className="reserva-bloque">
      <h1 ref={titulo} tabIndex={-1}>Este enlace no está disponible</h1>
      <p>Puede que el turno ya haya pasado o que el enlace ya no sea válido.</p>
      <p>Si necesitás ayuda, comunicate con el consultorio.</p>
    </div>
  );
}
