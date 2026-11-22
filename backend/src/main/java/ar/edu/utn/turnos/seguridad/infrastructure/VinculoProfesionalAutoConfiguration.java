package ar.edu.utn.turnos.seguridad.infrastructure;

import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalVinculadoConsulta;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Vínculo usuario → profesional por defecto (C-13). Es una autoconfiguración para que Spring la evalúe
 * después de las configuraciones de la aplicación y de los tests: si un test declara su propio
 * {@link ProfesionalVinculadoConsulta} (como ProfesionalesTitularidadIT y BloqueosApiIT), este no se registra
 * y no hay dos candidatos. Se declara en
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 * Los slices de test ({@code @WebMvcTest}) no cargan autoconfiguraciones propias, así que ahí no se pide
 * el repositorio.
 */
@AutoConfiguration
public class VinculoProfesionalAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ProfesionalVinculadoConsulta.class)
    ProfesionalVinculadoConsulta vinculoProfesionalUsuario(UsuarioJpaRepository usuarios) {
        return new VinculoProfesionalUsuario(usuarios);
    }
}
