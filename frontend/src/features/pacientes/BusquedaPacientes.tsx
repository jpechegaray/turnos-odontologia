import { useState, type FormEvent } from "react";
import { Boton, Campo, Icono } from "../../shared/ui";
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
    <form role="search" aria-label="Buscar pacientes" className="filtros" onSubmit={enviar}>
      <Campo
        etiqueta="Apellido"
        etiquetaOculta
        placeholder="Apellido"
        name="apellido"
        value={filtros.apellido}
        onChange={(evento) => actualizarFiltros({ ...filtros, apellido: evento.target.value })}
      />
      <Campo
        etiqueta="Nombre"
        etiquetaOculta
        placeholder="Nombre"
        name="nombre"
        value={filtros.nombre}
        onChange={(evento) => actualizarFiltros({ ...filtros, nombre: evento.target.value })}
      />
      <Campo
        etiqueta="DNI"
        etiquetaOculta
        placeholder="DNI exacto"
        name="dni"
        value={filtros.dni}
        onChange={(evento) => actualizarFiltros({ ...filtros, dni: evento.target.value })}
      />
      <Campo
        etiqueta="Email"
        etiquetaOculta
        placeholder="Email exacto"
        name="email"
        value={filtros.email}
        onChange={(evento) => actualizarFiltros({ ...filtros, email: evento.target.value })}
      />
      <Boton type="submit" variante="secundaria"><Icono nombre="buscar" />Buscar</Boton>
    </form>
  );
}
