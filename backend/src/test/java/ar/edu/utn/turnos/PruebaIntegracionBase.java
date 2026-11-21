package ar.edu.utn.turnos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base de las pruebas de integración: contexto completo, perfil {@code test} y PostgreSQL real.
 *
 * <p>Para no arrancar un contenedor nuevo por cada clase, las subclases deben preferir
 * {@code @Import} de configuraciones compartidas antes que {@code @MockitoBean} o propiedades
 * propias: cualquier cambio en la configuración del contexto crea un contexto (y un contenedor) nuevo.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(ContenedoresPrueba.class)
@AutoConfigureMockMvc
public abstract class PruebaIntegracionBase {

    @Autowired
    protected MockMvc mockMvc;
}
