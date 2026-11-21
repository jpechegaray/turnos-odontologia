package ar.edu.utn.turnos.configuracion.infrastructure;

import ar.edu.utn.turnos.configuracion.domain.ParametroConfiguracion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "configuracion")
public class Configuracion {

    @Id
    @Column(columnDefinition = "text")
    private String clave;

    @Column(nullable = false, columnDefinition = "text")
    private String valor;

    @Column(nullable = false, columnDefinition = "text")
    private String descripcion;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected Configuracion() {
    }

    Configuracion(ParametroConfiguracion parametro, Instant ahora) {
        clave = parametro.clave();
        valor = parametro.valorInicial();
        descripcion = parametro.descripcion();
        actualizadoEn = ahora;
    }

    public String clave() {
        return clave;
    }

    public void actualizar(String nuevoValor, Instant ahora) {
        if (!valor.equals(nuevoValor)) {
            valor = nuevoValor;
            actualizadoEn = ahora;
        }
    }
}
