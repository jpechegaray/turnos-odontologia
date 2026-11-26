import { ESTADOS, type EntradaRespuesta, type EstadoEntrada } from "./tipos";

function fechaCivil(fecha: string) {
  return fecha.split("-").reverse().join("/");
}

export function textoFechas(entrada: EntradaRespuesta) {
  return entrada.fechaDesde
    ? `${fechaCivil(entrada.fechaDesde)} al ${fechaCivil(entrada.fechaHasta)}`
    : `Hasta el ${fechaCivil(entrada.fechaHasta)}`;
}

export function textoFranja(entrada: EntradaRespuesta) {
  return entrada.franjaDesde && entrada.franjaHasta
    ? `${entrada.franjaDesde} a ${entrada.franjaHasta}`
    : "Sin preferencia";
}

export function textoEstado(estado: EstadoEntrada) {
  return ESTADOS.find((opcion) => opcion.valor === estado)?.etiqueta ?? estado;
}
