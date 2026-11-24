package ar.edu.utn.turnos.pacientes.web;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.pacientes.application.PacienteServicio;
import ar.edu.utn.turnos.pacientes.domain.Paciente;
import ar.edu.utn.turnos.pacientes.infrastructure.PacienteRepositorioJpa;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PacienteGestionIT extends PruebaIntegracionBase {

    @Autowired
    private PacienteServicio servicio;

    @Autowired
    private PacienteRepositorioJpa repositorio;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void limpiar() {
        repositorio.deleteAll();
    }

    private String json(Object obj) throws Exception {
        return mapper.writeValueAsString(obj);
    }

    // ------------------------------------------------------------------------
    // Ficha de paciente con datos mínimos
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Alta con email y sin teléfono")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void altaConEmailSinTelefono() throws Exception {
        PacienteRequest req = new PacienteRequest();
        req.setNombre("Paciente");
        req.setApellido("Uno");
        req.setEmail("paciente1@example.test");

        mockMvc.perform(post("/api/pacientes")
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.email").value("paciente1@example.test"))
                .andExpect(jsonPath("$.telefono").doesNotExist());
    }

    @Test
    @DisplayName("Alta con teléfono y sin email")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void altaConTelefonoSinEmail() throws Exception {
        PacienteRequest req = new PacienteRequest();
        req.setNombre("Paciente");
        req.setApellido("Dos");
        req.setTelefono("0000-000002");

        mockMvc.perform(post("/api/pacientes")
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.telefono").value("0000-000002"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    @DisplayName("La obra social se guarda como texto libre")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void laObraSocialSeGuardaComoTextoLibre() throws Exception {
        PacienteRequest req = new PacienteRequest();
        req.setNombre("Paciente");
        req.setApellido("Tres");
        req.setEmail("paciente3@example.test");
        req.setObraSocial("Obra Social Ficticia Nivel A");

        mockMvc.perform(post("/api/pacientes")
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.obraSocial").value("Obra Social Ficticia Nivel A"));
    }

    @Test
    @DisplayName("Alta sin nombre o apellido se rechaza")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void altaSinNombreOApellidoSeRechaza() throws Exception {
        PacienteRequest req = new PacienteRequest();
        req.setNombre("Paciente");
        req.setApellido("");
        req.setEmail("paciente4@example.test");

        mockMvc.perform(post("/api/pacientes")
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores[*].campo").value(hasItem("apellido")));
    }

    // ------------------------------------------------------------------------
    // Medio de contacto obligatorio
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Alta sin email ni teléfono se rechaza")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void altaSinEmailNiTelefonoSeRechaza() throws Exception {
        PacienteRequest req = new PacienteRequest();
        req.setNombre("Paciente");
        req.setApellido("Cinco");

        mockMvc.perform(post("/api/pacientes")
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.codigo").value("RN-32"));
    }

    @Test
    @DisplayName("Edición que quita el único medio de contacto se rechaza")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void edicionQueQuitaElUnicoMedioDeContactoSeRechaza() throws Exception {
        Paciente p = Paciente.nuevo("Paciente", "Seis", null, "paciente6@example.test", null, null, null, null);
        Paciente guardado = servicio.crear(p);

        PacienteRequest req = new PacienteRequest();
        req.setNombre("Paciente");
        req.setApellido("Seis");
        req.setEmail(null);
        req.setTelefono(null);

        mockMvc.perform(put("/api/pacientes/{id}", guardado.id())
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("RN-32"));

        Paciente despues = servicio.obtener(guardado.id());
        org.assertj.core.api.Assertions.assertThat(despues.email()).isEqualTo("paciente6@example.test");
    }

    @Test
    @DisplayName("Edición que reemplaza email por teléfono se acepta")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void edicionQueReemplazaEmailPorTelefonoSeAcepta() throws Exception {
        Paciente p = Paciente.nuevo("Paciente", "Siete", null, "paciente7@example.test", null, null, null, null);
        Paciente guardado = servicio.crear(p);

        PacienteRequest req = new PacienteRequest();
        req.setNombre("Paciente");
        req.setApellido("Siete");
        req.setEmail(null);
        req.setTelefono("0000-000007");

        mockMvc.perform(put("/api/pacientes/{id}", guardado.id())
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("0000-000007"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    // ------------------------------------------------------------------------
    // DNI opcional y único
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Un DNI repetido devuelve 409")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void unDniRepetidoDevuelve409() throws Exception {
        Paciente p = Paciente.nuevo("Paciente", "Ocho", "00000001", "paciente8@example.test", null, null, null, null);
        servicio.crear(p);

        PacienteRequest req = new PacienteRequest();
        req.setNombre("Otro");
        req.setApellido("Paciente");
        req.setDni("00000001");
        req.setEmail("otro@example.test");

        mockMvc.perform(post("/api/pacientes")
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("RN-32"));
    }

    @Test
    @DisplayName("Varios pacientes pueden no tener DNI")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void variosPacientesPuedenNoTenerDni() throws Exception {
        servicio.crear(Paciente.nuevo("P1", "Uno", null, "p1@example.test", null, null, null, null));
        servicio.crear(Paciente.nuevo("P2", "Dos", null, "p2@example.test", null, null, null, null));

        PacienteRequest req = new PacienteRequest();
        req.setNombre("P3");
        req.setApellido("Tres");
        req.setEmail("p3@example.test");

        mockMvc.perform(post("/api/pacientes")
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("El DNI de una ficha inactiva queda libre para una ficha nueva")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void elDniDeUnaFichaInactivaQuedaLibre() throws Exception {
        Paciente p = servicio.crear(Paciente.nuevo("Inactivo", "Paciente", "00000002", "inactivo@example.test", null, null, null, null));
        servicio.darDeBaja(p.id());

        PacienteRequest req = new PacienteRequest();
        req.setNombre("Nuevo");
        req.setApellido("Paciente");
        req.setDni("00000002");
        req.setEmail("nuevo@example.test");

        mockMvc.perform(post("/api/pacientes")
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Un DNI en blanco se trata como no informado")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void unDniEnBlancoSeTrataComoNoInformado() throws Exception {
        servicio.crear(Paciente.nuevo("Existente", "Paciente", null, "existente@example.test", null, null, null, null));

        PacienteRequest req = new PacienteRequest();
        req.setNombre("Nuevo");
        req.setApellido("Paciente");
        req.setDni("   ");
        req.setEmail("nuevo_sin_dni@example.test");

        mockMvc.perform(post("/api/pacientes")
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dni").doesNotExist());
    }

    // ------------------------------------------------------------------------
    // Búsqueda de pacientes
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Búsqueda por apellido sin distinguir mayúsculas")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void busquedaPorApellidoSinDistinguirMayusculas() throws Exception {
        servicio.crear(Paciente.nuevo("Mario", "Gomez", null, "mario@example.test", null, null, null, null));

        mockMvc.perform(get("/api/pacientes").param("apellido", "gomez"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].apellido").value("Gomez"));
    }

    @Test
    @DisplayName("Búsqueda por nombre con coincidencia parcial")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void busquedaPorNombreConCoincidenciaParcial() throws Exception {
        servicio.crear(Paciente.nuevo("Mariana", "Perez", null, "mariana@example.test", null, null, null, null));

        mockMvc.perform(get("/api/pacientes").param("nombre", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Mariana"));
    }

    @Test
    @DisplayName("Búsqueda por DNI exacto")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void busquedaPorDniExacto() throws Exception {
        servicio.crear(Paciente.nuevo("Carlos", "Lopez", "00000003", "carlos@example.test", null, null, null, null));
        servicio.crear(Paciente.nuevo("Diego", "Lopez", "00000004", "diego@example.test", null, null, null, null));

        mockMvc.perform(get("/api/pacientes").param("dni", "00000003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].dni").value("00000003"));
    }

    @Test
    @DisplayName("Búsqueda por email sin distinguir mayúsculas")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void busquedaPorEmailSinDistinguirMayusculas() throws Exception {
        servicio.crear(Paciente.nuevo("Elena", "Suarez", null, "paciente1@example.test", null, null, null, null));

        mockMvc.perform(get("/api/pacientes").param("email", "PACIENTE1@EXAMPLE.TEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("paciente1@example.test"));
    }

    @Test
    @DisplayName("Los filtros se combinan")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void losFiltrosSeCombinan() throws Exception {
        servicio.crear(Paciente.nuevo("Ana", "Gomez", null, "ana@example.test", null, null, null, null));
        servicio.crear(Paciente.nuevo("Pedro", "Gomez", null, "pedro@example.test", null, null, null, null));

        mockMvc.perform(get("/api/pacientes")
                .param("apellido", "Gomez")
                .param("nombre", "Ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Ana"))
                .andExpect(jsonPath("$[0].apellido").value("Gomez"));
    }

    @Test
    @DisplayName("Una búsqueda sin filtros devuelve las fichas activas")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void unaBusquedaSinFiltrosDevuelveLasFichasActivas() throws Exception {
        servicio.crear(Paciente.nuevo("Activo", "Uno", null, "act1@example.test", null, null, null, null));
        servicio.crear(Paciente.nuevo("Activo", "Dos", null, "act2@example.test", null, null, null, null));
        Paciente p3 = servicio.crear(Paciente.nuevo("Baja", "Tres", null, "baja3@example.test", null, null, null, null));
        servicio.darDeBaja(p3.id());

        mockMvc.perform(get("/api/pacientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].activo").value(everyItem(is(true))));
    }

    @Test
    @DisplayName("Una búsqueda sin coincidencias devuelve una lista vacía")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void unaBusquedaSinCoincidenciasDevuelveUnaListaVacia() throws Exception {
        mockMvc.perform(get("/api/pacientes").param("dni", "99999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Un paciente inactivo no aparece en la búsqueda")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void unPacienteInactivoNoApareceEnLaBusqueda() throws Exception {
        Paciente p = servicio.crear(Paciente.nuevo("Baja", "Gomez", null, "baja_gomez@example.test", null, null, null, null));
        servicio.darDeBaja(p.id());

        mockMvc.perform(get("/api/pacientes").param("apellido", "Gomez"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ------------------------------------------------------------------------
    // Baja lógica de la ficha
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("La baja deja la ficha inactiva y consultable")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void laBajaDejaLaFichaInactivaYConsultable() throws Exception {
        Paciente p = servicio.crear(Paciente.nuevo("Baja", "Test", null, "baja_test@example.test", null, null, null, null));

        mockMvc.perform(delete("/api/pacientes/{id}", p.id()).with(csrfValido()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pacientes/{id}", p.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    @DisplayName("Una ficha dada de baja no se puede consultar como activa")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void unaFichaDadaDeBajaNoSePuedeConsultarComoActiva() throws Exception {
        Paciente p = servicio.crear(Paciente.nuevo("Baja", "Consulta", null, "baja_cons@example.test", null, null, null, null));
        servicio.darDeBaja(p.id());

        mockMvc.perform(get("/api/pacientes/{id}", p.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    @DisplayName("Dar de baja una ficha inexistente devuelve 404")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void darDeBajaUnaFichaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(delete("/api/pacientes/{id}", 999999L).with(csrfValido()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // ------------------------------------------------------------------------
    // Permisos por rol en la ficha de paciente
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Recepción crea, edita y busca fichas")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void recepcionCreaEditaYBuscaFichas() throws Exception {
        PacienteRequest altaReq = new PacienteRequest();
        altaReq.setNombre("Recepcion");
        altaReq.setApellido("Test");
        altaReq.setEmail("rec_test@example.test");

        String res = mockMvc.perform(post("/api/pacientes")
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(altaReq)))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        Long id = mapper.readTree(res).get("id").asLong();

        PacienteRequest editReq = new PacienteRequest();
        editReq.setNombre("RecepcionMod");
        editReq.setApellido("TestMod");
        editReq.setEmail("rec_mod@example.test");

        mockMvc.perform(put("/api/pacientes/{id}", id)
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(editReq)))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/api/pacientes").param("apellido", "TestMod"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Recepción no puede dar de baja una ficha")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void recepcionNoPuedeDarDeBajaUnaFicha() throws Exception {
        Paciente p = servicio.crear(Paciente.nuevo("NoBaja", "Recep", null, "nobaja@example.test", null, null, null, null));

        mockMvc.perform(delete("/api/pacientes/{id}", p.id()).with(csrfValido()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        Paciente despues = servicio.obtener(p.id());
        org.assertj.core.api.Assertions.assertThat(despues.activo()).isTrue();
    }

    @Test
    @DisplayName("Administrador da de baja una ficha")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void administradorDaDeBajaUnaFicha() throws Exception {
        Paciente p = servicio.crear(Paciente.nuevo("BajaAdmin", "Test", null, "baja_admin@example.test", null, null, null, null));

        mockMvc.perform(delete("/api/pacientes/{id}", p.id()).with(csrfValido()))
                .andExpect(status().isNoContent());

        Paciente despues = servicio.obtener(p.id());
        org.assertj.core.api.Assertions.assertThat(despues.activo()).isFalse();
    }

    @Test
    @DisplayName("El odontólogo no accede a las fichas")
    @WithMockUser(username = "odontologo", roles = {"ODONTOLOGO"})
    void elOdontologoNoAccedeALasFichas() throws Exception {
        Paciente p = servicio.crear(Paciente.nuevo("Ficha", "Privada", null, "privada@example.test", null, null, null, null));

        PacienteRequest req = new PacienteRequest();
        req.setNombre("Intento");
        req.setApellido("Odonto");
        req.setEmail("intento@example.test");

        mockMvc.perform(get("/api/pacientes"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/api/pacientes/{id}", p.id()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(post("/api/pacientes")
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("Una petición anónima recibe 401")
    void unaPeticionAnonimaRecibe401() throws Exception {
        mockMvc.perform(get("/api/pacientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // ------------------------------------------------------------------------
    // Errores sin datos de otros pacientes (RN-35)
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Un DNI duplicado no expone los datos del otro paciente")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void unDniDuplicadoNoExponeLosDatosDelOtroPaciente() throws Exception {
        servicio.crear(Paciente.nuevo("NombreSecreto", "ApellidoSecreto", "00000001", "secreto@example.test", "0000-000001", null, null, null));

        PacienteRequest req = new PacienteRequest();
        req.setNombre("Nuevo");
        req.setApellido("Paciente");
        req.setDni("00000001");
        req.setEmail("nuevo@example.test");

        mockMvc.perform(post("/api/pacientes")
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("RN-32"))
                .andExpect(content().string(not(containsString("NombreSecreto"))))
                .andExpect(content().string(not(containsString("ApellidoSecreto"))))
                .andExpect(content().string(not(containsString("secreto@example.test"))))
                .andExpect(content().string(not(containsString("0000-000001"))));
    }

    // ------------------------------------------------------------------------
    // Datos complementarios de la ficha (número de afiliado, notas, creación)
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Alta con número de afiliado y notas")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void altaConNumeroDeAfiliadoYNotas() throws Exception {
        PacienteRequest req = new PacienteRequest();
        req.setNombre("Paciente");
        req.setApellido("Ocho");
        req.setEmail("paciente8b@example.test");
        req.setNroAfiliado("AF-000008");
        req.setNotas("Alergia ficticia a la penicilina");

        mockMvc.perform(post("/api/pacientes")
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nroAfiliado").value("AF-000008"))
                .andExpect(jsonPath("$.notas").value("Alergia ficticia a la penicilina"));
    }

    @Test
    @DisplayName("Las notas demasiado largas se rechazan")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void lasNotasDemasiadoLargasSeRechazan() throws Exception {
        PacienteRequest req = new PacienteRequest();
        req.setNombre("Paciente");
        req.setApellido("Nueve");
        req.setEmail("paciente9@example.test");
        req.setNotas("x".repeat(1001));

        mockMvc.perform(post("/api/pacientes")
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errores[*].campo").value(hasItem("notas")));
    }

    @Test
    @DisplayName("La edición actualiza el número de afiliado y las notas")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void laEdicionActualizaNumeroDeAfiliadoYNotas() throws Exception {
        Paciente p = servicio.crear(Paciente.nuevo("Paciente", "Diez", null, "paciente10@example.test", null, null, "AF-1", "Nota inicial"));

        PacienteRequest req = new PacienteRequest();
        req.setNombre("Paciente");
        req.setApellido("Diez");
        req.setEmail("paciente10@example.test");
        req.setNroAfiliado("AF-2");
        req.setNotas("Nota corregida");

        mockMvc.perform(put("/api/pacientes/{id}", p.id())
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nroAfiliado").value("AF-2"))
                .andExpect(jsonPath("$.notas").value("Nota corregida"));
    }

    @Test
    @DisplayName("El alta registra la fecha de creación y la edición no la cambia")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void elAltaRegistraLaFechaDeCreacionYLaEdicionNoLaCambia() throws Exception {
        PacienteRequest req = new PacienteRequest();
        req.setNombre("Paciente");
        req.setApellido("Once");
        req.setEmail("paciente11@example.test");

        String creada = mockMvc.perform(post("/api/pacientes")
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creadoEn").isString())
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(creada).get("id").asLong();
        String creadoEn = mapper.readTree(creada).get("creadoEn").asText();

        req.setApellido("OnceEditado");
        mockMvc.perform(put("/api/pacientes/{id}", id)
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creadoEn").value(creadoEn));
    }

    @Test
    @DisplayName("La respuesta no expone el consentimiento de datos ni acepta informarlo")
    @WithMockUser(username = "recept", roles = {"RECEPCION"})
    void laRespuestaNoExponeElConsentimientoDeDatos() throws Exception {
        String cuerpo = """
                {"nombre":"Paciente","apellido":"Doce","email":"paciente12@example.test",
                 "consentimientoDatosEn":"2020-01-01T00:00:00Z"}
                """;

        String creada = mockMvc.perform(post("/api/pacientes")
                .with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consentimientoDatosEn").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(creada).get("id").asLong();

        org.assertj.core.api.Assertions.assertThat(servicio.obtener(id).consentimientoDatosEn()).isNull();
    }
}
