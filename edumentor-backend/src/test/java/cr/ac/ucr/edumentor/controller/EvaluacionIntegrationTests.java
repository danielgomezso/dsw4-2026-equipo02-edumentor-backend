package cr.ac.ucr.edumentor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cr.ac.ucr.edumentor.dto.EvaluacionRequest;
import cr.ac.ucr.edumentor.entity.*;
import cr.ac.ucr.edumentor.repository.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
class EvaluacionIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepository usuarios;
    @Autowired MateriaRepository materias;
    @Autowired SesionRepository sesiones;
    MockMvc mvc;
    Usuario estudiante;
    Usuario tutor;
    Materia materia;
    Long sesionId;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        estudiante = usuarios.findByEmailIgnoreCase("estudiante@edumentor.test").orElseThrow();
        tutor = usuarios.findByEmailIgnoreCase("tutor@edumentor.test").orElseThrow();
        materia = new Materia();
        materia.setCodigoMateria("EVA001");
        materia.setNombreMateria("Evaluaciones");
        materia.setCarrera("Informatica");
        materia.setCreditos(4);
        materias.saveAndFlush(materia);
        sesionId = sesion(tutor, EstadoSesion.COMPLETED);
    }

    private Long sesion(Usuario profesor, EstadoSesion estado) {
        SesionTutoria sesion = new SesionTutoria();
        sesion.setEstudiante(estudiante);
        sesion.setTutor(profesor);
        sesion.setMateria(materia);
        sesion.setFechaHora(LocalDateTime.of(2020, 6, 10, 10, 0));
        sesion.setDuracionMinutos(60);
        sesion.setEstado(estado);
        sesion.setModalidad(Modalidad.VIRTUAL);
        return sesiones.saveAndFlush(sesion).getId();
    }

    private String ruta(Long id) { return "/api/v1/sesiones/" + id + "/evaluacion"; }
    private String promedio(Long id) { return "/api/v1/tutores/" + id + "/calificacion"; }

    private void evaluar(Long id, int estrellas) throws Exception {
        mvc.perform(post(ruta(id)).with(user(estudiante)).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new EvaluacionRequest(estrellas, "  Muy util  "))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.comentario").value("Muy util"))
                .andExpect(jsonPath("$.fechaEvaluacion").isNotEmpty());
    }

    @Test
    void guardaEvaluacionYCambiaSesionAReviewed() throws Exception {
        evaluar(sesionId, 5);
        mvc.perform(get("/api/v1/sesiones/" + sesionId).with(user(estudiante)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("REVIEWED"));
        mvc.perform(get(ruta(sesionId)).with(user(tutor))).andExpect(status().isOk())
                .andExpect(jsonPath("$.calificacionEstrellas").value(5));
        Usuario coordinador = usuarios.findByEmailIgnoreCase("coordinador@edumentor.test").orElseThrow();
        mvc.perform(get(ruta(sesionId)).with(user(coordinador))).andExpect(status().isOk());
    }

    @Test
    void promedioSeActualizaConDecimalesYSeparaTutores() throws Exception {
        mvc.perform(get(promedio(tutor.getId())).with(user(estudiante)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalEvaluaciones").value(0))
                .andExpect(jsonPath("$.promedio").value(org.hamcrest.Matchers.nullValue()));
        evaluar(sesionId, 3);
        mvc.perform(get(promedio(tutor.getId())).with(user(estudiante)))
                .andExpect(jsonPath("$.promedio").value(3.0)).andExpect(jsonPath("$.totalEvaluaciones").value(1));
        evaluar(sesion(tutor, EstadoSesion.COMPLETED), 4);
        Usuario otroTutor = usuarios.saveAndFlush(new Usuario("EVAT2", "evat2@example.invalid", "NO_USAR", "Otro", "Tutor", Rol.TUTOR));
        evaluar(sesion(otroTutor, EstadoSesion.COMPLETED), 5);
        mvc.perform(get(promedio(tutor.getId())).with(user(estudiante)))
                .andExpect(jsonPath("$.promedio").value(3.5)).andExpect(jsonPath("$.totalEvaluaciones").value(2));
        mvc.perform(get(promedio(otroTutor.getId())).with(user(estudiante)))
                .andExpect(jsonPath("$.promedio").value(5.0)).andExpect(jsonPath("$.totalEvaluaciones").value(1));
    }

    @Test
    void rechazaSegundaEvaluacion() throws Exception {
        evaluar(sesionId, 5);
        mvc.perform(post(ruta(sesionId)).with(user(estudiante)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calificacionEstrellas\":1}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        mvc.perform(get(ruta(sesionId)).with(user(estudiante)))
                .andExpect(jsonPath("$.calificacionEstrellas").value(5));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSesion.class, names = "COMPLETED", mode = EnumSource.Mode.EXCLUDE)
    void rechazaEvaluacionEnOtrosEstados(EstadoSesion estado) throws Exception {
        Long id = sesion(tutor, estado);
        mvc.perform(post(ruta(id)).with(user(estudiante)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calificacionEstrellas\":5}"))
                .andExpect(status().isConflict()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void validaEstrellasYLongitudDeComentario() throws Exception {
        for (Integer estrellas : new Integer[]{null, 0, 6}) {
            mvc.perform(post(ruta(sesionId)).with(user(estudiante)).contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(new EvaluacionRequest(estrellas, null))))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post(ruta(sesionId)).with(user(estudiante)).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new EvaluacionRequest(5, "x".repeat(2001)))))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/sesiones/" + sesionId).with(user(estudiante)))
                .andExpect(jsonPath("$.estado").value("COMPLETED"));
    }

    @Test
    void permiteComentarioOpcional() throws Exception {
        mvc.perform(post(ruta(sesionId)).with(user(estudiante)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calificacionEstrellas\":1}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.comentario").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void protegeRolesYEvaluacionesAjenas() throws Exception {
        Usuario otro = usuarios.saveAndFlush(new Usuario("EVAE2", "evae2@example.invalid", "NO_USAR", "Otro", "Estudiante", Rol.ESTUDIANTE));
        Usuario coordinador = usuarios.findByEmailIgnoreCase("coordinador@edumentor.test").orElseThrow();
        for (Usuario noPermitido : new Usuario[]{tutor, coordinador, otro}) {
            mvc.perform(post(ruta(sesionId)).with(user(noPermitido)).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"calificacionEstrellas\":5}"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get(ruta(sesionId)).with(user(otro))).andExpect(status().isForbidden());
    }

    @Test
    void compruebaAutenticacionYRegistrosInexistentes() throws Exception {
        mvc.perform(get(ruta(sesionId))).andExpect(status().isUnauthorized());
        mvc.perform(get(promedio(tutor.getId()))).andExpect(status().isUnauthorized());
        mvc.perform(post(ruta(sesionId)).contentType(MediaType.APPLICATION_JSON).content("{\"calificacionEstrellas\":5}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(ruta(sesionId)).with(user(tutor))).andExpect(status().isNotFound());
        mvc.perform(post(ruta(Long.MAX_VALUE)).with(user(estudiante)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calificacionEstrellas\":5}"))
                .andExpect(status().isNotFound());
        mvc.perform(get(promedio(Long.MAX_VALUE)).with(user(estudiante))).andExpect(status().isNotFound());
        mvc.perform(get(promedio(estudiante.getId())).with(user(estudiante))).andExpect(status().isBadRequest());
    }
}
