package cr.ac.ucr.edumentor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cr.ac.ucr.edumentor.dto.RecursoRequest;
import cr.ac.ucr.edumentor.entity.*;
import cr.ac.ucr.edumentor.repository.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class RecursoIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepository usuarios;
    @Autowired MateriaRepository materias;
    @Autowired SesionRepository sesiones;
    MockMvc mvc;
    Usuario estudiante;
    Usuario tutor;
    String ruta;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        estudiante = usuarios.findByEmailIgnoreCase("estudiante@edumentor.test").orElseThrow();
        tutor = usuarios.findByEmailIgnoreCase("tutor@edumentor.test").orElseThrow();
        Materia materia = new Materia();
        materia.setCodigoMateria("REC001");
        materia.setNombreMateria("Recursos");
        materia.setCarrera("Informatica");
        materia.setCreditos(4);
        materias.saveAndFlush(materia);
        SesionTutoria sesion = new SesionTutoria();
        sesion.setEstudiante(estudiante);
        sesion.setTutor(tutor);
        sesion.setMateria(materia);
        sesion.setFechaHora(LocalDateTime.of(2030, 6, 10, 10, 0));
        sesion.setDuracionMinutos(60);
        sesion.setEstado(EstadoSesion.CONFIRMED);
        sesion.setModalidad(Modalidad.VIRTUAL);
        ruta = "/api/v1/sesiones/" + sesiones.saveAndFlush(sesion).getId() + "/recursos";
    }

    private String datos(String enlace) throws Exception {
        return mapper.writeValueAsString(new RecursoRequest(" Guia de ejercicios ", enlace, "PDF"));
    }

    @Test
    void participantesCompartenYConsultanEnlaces() throws Exception {
        for (Usuario participante : new Usuario[]{estudiante, tutor}) {
            mvc.perform(post(ruta).with(user(participante)).contentType(MediaType.APPLICATION_JSON)
                            .content(datos("https://example.com/guia.pdf")))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.titulo").value("Guia de ejercicios"))
                    .andExpect(jsonPath("$.sesionId").isNumber());
        }
        mvc.perform(get(ruta).with(user(estudiante))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].urlRecurso").value("https://example.com/guia.pdf"));
        Usuario coordinador = usuarios.findByEmailIgnoreCase("coordinador@edumentor.test").orElseThrow();
        mvc.perform(get(ruta).with(user(coordinador))).andExpect(status().isOk());
        mvc.perform(post(ruta).with(user(coordinador)).contentType(MediaType.APPLICATION_JSON)
                        .content(datos("https://example.com/guia.pdf"))).andExpect(status().isForbidden());
    }

    @Test
    void impideAccesoASesionesAjenas() throws Exception {
        Usuario otro = usuarios.saveAndFlush(new Usuario("RECOTRO", "recotro@example.invalid", "NO_USAR", "Otro", "Tutor", Rol.TUTOR));
        mvc.perform(get(ruta).with(user(otro))).andExpect(status().isForbidden());
        mvc.perform(post(ruta).with(user(otro)).contentType(MediaType.APPLICATION_JSON)
                        .content(datos("https://example.com/guia.pdf"))).andExpect(status().isForbidden());
    }

    @Test
    void rechazaEnlacesInvalidos() throws Exception {
        for (String enlace : new String[]{"javascript:alert(1)", "ftp://example.com/file", "https://", "texto sin enlace"}) {
            mvc.perform(post(ruta).with(user(tutor)).contentType(MediaType.APPLICATION_JSON).content(datos(enlace)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        mvc.perform(get(ruta).with(user(tutor))).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void validaCamposObligatoriosYLongitud() throws Exception {
        mvc.perform(post(ruta).with(user(tutor)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        String largo = mapper.writeValueAsString(new RecursoRequest("x".repeat(201), "https://example.com", "PDF"));
        mvc.perform(post(ruta).with(user(tutor)).contentType(MediaType.APPLICATION_JSON).content(largo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exigeAutenticacionYCompruebaSesion() throws Exception {
        mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
        mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(datos("https://example.com")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/sesiones/999999/recursos").with(user(tutor))).andExpect(status().isNotFound());
    }
}
