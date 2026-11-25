import { useState } from "react";
import { api } from "../../../shared/api/clienteApi";
import { Alerta, Boton, Tabla, Toast, type ColumnaTabla } from "../../../shared/ui";
import { cuerpoModificacion } from "./cuerpos";
import { AccionesFila } from "./AccionesFila";
import { requiereRecarga, traducirErrorUsuario } from "./errores";
import { FormularioUsuario, type DatosFormularioUsuario } from "./FormularioUsuario";
import { opcionesProfesional } from "./opciones";
import { ETIQUETAS_ROL } from "./roles";
import type { ProfesionalVinculable, UsuarioAlta, UsuarioRespuesta } from "./tipos";
import { useUsuarios } from "./useUsuarios";

const MARCA_VACIO = "—";

function nombreProfesional(profesionales: readonly ProfesionalVinculable[], id: number | null): string {
  const profesional = id === null ? undefined : profesionales.find((p) => p.id === id);
  return profesional ? `${profesional.apellido}, ${profesional.nombre}` : MARCA_VACIO;
}

export function PaginaUsuarios() {
  const { usuarios, profesionales, cargando, errorCarga, recargar } = useUsuarios();
  const [aviso, actualizarAviso] = useState<string>();
  const [editando, actualizarEditando] = useState<UsuarioRespuesta>();
  const [confirmandoDesactivacion, actualizarConfirmandoDesactivacion] = useState<number>();
  const [errorAccion, actualizarErrorAccion] = useState<string>();

  /** Ejecuta una escritura; si el error lo pide (D5), vuelve a consultar antes de propagarlo. */
  async function escribir(operacion: () => Promise<unknown>) {
    actualizarAviso(undefined);
    try {
      await operacion();
    } catch (error) {
      if (requiereRecarga(error)) await recargar();
      throw error;
    }
  }

  async function crear(datos: DatosFormularioUsuario) {
    const alta: UsuarioAlta = { ...datos, password: datos.password ?? "" };
    await escribir(() => api.post<UsuarioRespuesta>("/usuarios", alta));
    actualizarAviso("Usuario agregado.");
    await recargar();
  }

  async function modificar(usuario: UsuarioRespuesta, datos: DatosFormularioUsuario) {
    await escribir(() => api.put<UsuarioRespuesta>(`/usuarios/${usuario.id}`, cuerpoModificacion(datos, usuario.activo)));
    actualizarEditando(undefined);
    actualizarAviso("Usuario actualizado.");
    await recargar();
  }

  /** Desactiva con `DELETE` o reactiva con `PUT`; recarga después de cada respuesta, también ante un error (D6). */
  async function cambiarEstado(usuario: UsuarioRespuesta, activo: boolean) {
    actualizarAviso(undefined);
    actualizarErrorAccion(undefined);
    try {
      if (activo) await api.put<UsuarioRespuesta>(`/usuarios/${usuario.id}`, cuerpoModificacion(usuario, true));
      else await api.delete(`/usuarios/${usuario.id}`);
      actualizarAviso(activo ? "Usuario reactivado." : "Usuario desactivado.");
    } catch (error) {
      actualizarErrorAccion(traducirErrorUsuario(error).mensaje);
    } finally {
      actualizarConfirmandoDesactivacion(undefined);
      await recargar();
    }
  }

  const columnas: readonly ColumnaTabla<UsuarioRespuesta>[] = [
    { clave: "email", titulo: "Email", celda: (u) => u.email },
    { clave: "nombre", titulo: "Nombre", celda: (u) => u.nombre },
    { clave: "rol", titulo: "Rol", celda: (u) => ETIQUETAS_ROL[u.rol] },
    { clave: "profesional", titulo: "Profesional", celda: (u) => nombreProfesional(profesionales, u.profesionalId) },
    { clave: "estado", titulo: "Estado", celda: (u) => (u.activo ? "Activo" : "Inactivo") },
    {
      clave: "acciones",
      titulo: "Acciones",
      celda: (u) => (
        <AccionesFila
          usuario={u}
          confirmandoDesactivacion={confirmandoDesactivacion === u.id}
          onEditar={actualizarEditando}
          onReactivar={(usuario) => void cambiarEstado(usuario, true)}
          onSolicitarDesactivacion={(usuario) => actualizarConfirmandoDesactivacion(usuario.id)}
          onConfirmarDesactivacion={(usuario) => void cambiarEstado(usuario, false)}
          onCancelarDesactivacion={() => actualizarConfirmandoDesactivacion(undefined)}
        />
      ),
    },
  ];

  return (
    <section>
      <h2>Usuarios</h2>
      <FormularioUsuario
        key={editando?.id ?? "nuevo"}
        valorInicial={editando}
        profesionales={opcionesProfesional(profesionales, usuarios ?? [], editando?.id)}
        onEnviar={(datos) => (editando ? modificar(editando, datos) : crear(datos))}
        onCancelar={editando ? () => actualizarEditando(undefined) : undefined}
      />
      {aviso && <Toast mensaje={aviso} />}
      {errorAccion && <Alerta tipo="peligro" titulo={errorAccion} />}
      {cargando ? (
        <p role="status">Cargando…</p>
      ) : errorCarga ? (
        <Alerta tipo="peligro" titulo="No pudimos cargar los usuarios.">
          <Boton variante="secundaria" onClick={() => void recargar()}>Reintentar</Boton>
        </Alerta>
      ) : (
        <Tabla
          titulo="Usuarios"
          columnas={columnas}
          filas={usuarios ?? []}
          claveFila={(u) => u.id}
          mensajeVacio="No hay usuarios cargados."
        />
      )}
    </section>
  );
}
