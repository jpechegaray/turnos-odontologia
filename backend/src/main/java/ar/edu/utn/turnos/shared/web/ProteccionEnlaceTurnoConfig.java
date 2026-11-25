package ar.edu.utn.turnos.shared.web;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration(proxyBeanMethods = false)
class ProteccionEnlaceTurnoConfig {
    @Bean
    FilterRegistrationBean<Filtro> proteccionEnlaceTurno() {
        var registro = new FilterRegistrationBean<>(new Filtro());
        registro.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
        // También cubre rechazos de C-17, cuyo filtro se registra en HIGHEST_PRECEDENCE + 20.
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registro;
    }

    @Bean
    DefaultErrorAttributes atributosDeError() {
        return new DefaultErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(WebRequest request, ErrorAttributeOptions options) {
                if (request instanceof ServletWebRequest servlet
                        && RutaConTokenDeTurno.coincide(servlet.getRequest())) {
                    // El despacho de contenedor a /error no pasa por el manejador de errores MVC.
                    var atributos = super.getErrorAttributes(request, ErrorAttributeOptions.defaults());
                    atributos.remove("path");
                    return atributos;
                }
                return super.getErrorAttributes(request, options);
            }
        };
    }

    static final class Filtro extends OncePerRequestFilter {
        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return !RutaConTokenDeTurno.coincide(request);
        }

        @Override
        protected boolean shouldNotFilterErrorDispatch() {
            return false;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
                throws ServletException, IOException {
            RutaConTokenDeTurno.marcar(request);
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            cadena.doFilter(request, response);
        }
    }
}
