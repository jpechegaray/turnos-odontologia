import { useState, type FormEvent } from "react";
import { Boton, Campo } from "../../shared/ui";
import { FILTROS_VACIOS } from "./consulta";
import type { FiltrosPacientes } from "./tipos";

export type BusquedaPacientesProps = Readonly<{
  onBuscar: (filtros: FiltrosPacientes) => void;
}>;

export function BusquedaPacientes({ onBuscar }: BusquedaPacientesProps) {
  const [filtros, actualizarFiltros] = useState<FiltrosPacientes>(FILTROS_VACIOS);

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    onBuscar(filtros);
  }

  return (
    <form role="search" aria-label="Buscar pacientes" onSubmit={enviar}>
      <Campo
        etiqueta="Apellido"
        name="apellido"
        value={filtros.apellido}
        onChange={(evento) => actualizarFiltros({ ...filtros, apellido: evento.target.value })}
      />
      <Campo
        etiqueta="Nombre"
        name="nombre"
        value={filtros.nombre}
        onChange={(evento) => actualizarFiltros({ ...filtros, nombre: evento.target.value })}
      />
      <Campo
        etiqueta="DNI"
        name="dni"
        ayuda="Coincidencia exacta."
        value={filtros.dni}
        onChange={(evento) => actualizarFiltros({ ...filtros, dni: evento.target.value })}
      />
      <Campo
        etiqueta="Email"
        name="email"
        ayuda="Coincidencia exacta."
        value={filtros.email}
        onChange={(evento) => actualizarFiltros({ ...filtros, email: evento.target.value })}
      />
      <Boton type="submit">Buscar</Boton>
    </form>
  );
}
