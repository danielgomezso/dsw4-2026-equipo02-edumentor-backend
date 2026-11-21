package cr.ac.ucr.edumentor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cr.ac.ucr.edumentor.dto.SesionRequest;
import cr.ac.ucr.edumentor.entity.*;
import cr.ac.ucr.edumentor.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
class SesionIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepository usuarios;
    @Autowired MateriaRepository materias;
    @Autowired DisponibilidadRepository disponibilidades;
    MockMvc mvc;
    Usuario estudiante;
    Usuario tutor;
    Usuario otroTutor;
    Materia materia;
    DisponibilidadTutor horario;
    final LocalDateTime fecha = LocalDate.now(ZoneId.of("America/Costa_Rica")).plusDays(7).atTime(10, 0);

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        estudiante = usuarios.findByEmailIgnoreCase("estudiante@edumentor.test").orElseThrow();
        tutor = usuarios.findByEmailIgnoreCase("tutor@edumentor.test").orElseThrow();
        otroTutor = usuarios.saveAndFlush(new Usuario("SESION_T2", "sesion_t2@example.invalid", "NO_USAR", "Otro", "Tutor", Rol.TUTOR));
        materia = new Materia();
        materia.setCodigoMateria("SES001");
        materia.setNombreMateria("Prueba de sesiones");
        materia.setCarrera("Informatica");
        materia.setCreditos(4);
        materias.saveAndFlush(materia);
        horario = horario(tutor);
        horario(otroTutor);
    }

    private DisponibilidadTutor horario(Usuario usuario) {
        DisponibilidadTutor bloque = new DisponibilidadTutor();
        bloque.setTutor(usuario);
        bloque.setDiaSemana(fecha.getDayOfWeek().getValue());
        bloque.setHoraInicio(LocalTime.of(9, 0));
        bloque.setHoraFin(LocalTime.of(13, 0));
        bloque.setEstaActivo(true);
        return disponibilidades.saveAndFlush(bloque);
    }

    private String datos(Usuario profesor, LocalDateTime inicio, int minutos) throws Exception {
        return mapper.writeValueAsString(new SesionRequest(profesor.getId(), materia.getId(), inicio, minutos, Modalidad.VIRTUAL));
    }

    private long solicitar(Usuario alumno, Usuario profesor, LocalDateTime inicio) throws Exception {
        String json = mvc.perform(post("/api/v1/sesiones").with(user(alumno))
                        .contentType(MediaType.APPLICATION_JSON).content(datos(profesor, inicio, 60)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("REQUESTED"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get("id").asLong();
    }

    @Test
    void solicitaConsultaYConfirma() throws Exception {
        long id = solicitar(estudiante, tutor, fecha);
        mvc.perform(get("/api/v1/sesiones/" + id).with(user(estudiante)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estudianteId").value(estudiante.getId()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(patch("/api/v1/sesiones/" + id + "/confirmar").with(user(tutor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMED"));
        mvc.perform(get("/api/v1/sesiones").with(user(tutor))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void rechazaTraslapeDelEstudianteConDistintosTutores() throws Exception {
        long primera = solicitar(estudiante, tutor, fecha);
        long segunda = solicitar(estudiante, otroTutor, fecha.plusMinutes(30));
        mvc.perform(patch("/api/v1/sesiones/" + primera + "/confirmar").with(user(tutor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMED"));
        mvc.perform(patch("/api/v1/sesiones/" + segunda + "/confirmar").with(user(otroTutor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("REJECTED"));
        mvc.perform(get("/api/v1/sesiones/" + segunda).with(user(estudiante)))
                .andExpect(jsonPath("$.estado").value("REJECTED"));
    }

    @Test
    void rechazaTraslapeDelTutorConDistintosEstudiantes() throws Exception {
        Usuario otro = usuarios.saveAndFlush(new Usuario("SESION_E2", "sesion_e2@example.invalid", "NO_USAR", "Otro", "Estudiante", Rol.ESTUDIANTE));
        long primera = solicitar(estudiante, tutor, fecha);
        long segunda = solicitar(otro, tutor, fecha.plusMinutes(30));
        mvc.perform(patch("/api/v1/sesiones/" + primera + "/confirmar").with(user(tutor)))
                .andExpect(jsonPath("$.estado").value("CONFIRMED"));
        mvc.perform(patch("/api/v1/sesiones/" + segunda + "/confirmar").with(user(tutor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("REJECTED"));
    }

    @Test
    void permiteSesionQueIniciaCuandoTerminaOtra() throws Exception {
        long primera = solicitar(estudiante, tutor, fecha);
        mvc.perform(patch("/api/v1/sesiones/" + primera + "/confirmar").with(user(tutor)))
                .andExpect(jsonPath("$.estado").value("CONFIRMED"));
        long siguiente = solicitar(estudiante, tutor, fecha.plusHours(1));
        mvc.perform(patch("/api/v1/sesiones/" + siguiente + "/confirmar").with(user(tutor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMED"));
    }

    @Test
    void noCreaSolicitudSobreHorarioYaConfirmado() throws Exception {
        long id = solicitar(estudiante, tutor, fecha);
        mvc.perform(patch("/api/v1/sesiones/" + id + "/confirmar").with(user(tutor))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/sesiones").with(user(estudiante))
                        .contentType(MediaType.APPLICATION_JSON).content(datos(tutor, fecha, 60)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void validaHorarioActivoDuracionFechaYCampos() throws Exception {
        for (LocalDateTime inicio : new LocalDateTime[]{fecha.withHour(8), fecha.withHour(12).withMinute(30), fecha.minusYears(1)}) {
            mvc.perform(post("/api/v1/sesiones").with(user(estudiante)).contentType(MediaType.APPLICATION_JSON)
                            .content(datos(tutor, inicio, 60)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").isNotEmpty());
        }
        mvc.perform(post("/api/v1/sesiones").with(user(estudiante)).contentType(MediaType.APPLICATION_JSON)
                        .content(datos(tutor, fecha, 0))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/sesiones").with(user(estudiante)).contentType(MediaType.APPLICATION_JSON)
                        .content("{}")).andExpect(status().isBadRequest());
        horario.setEstaActivo(false);
        disponibilidades.saveAndFlush(horario);
        mvc.perform(post("/api/v1/sesiones").with(user(estudiante)).contentType(MediaType.APPLICATION_JSON)
                        .content(datos(tutor, fecha, 60))).andExpect(status().isBadRequest());
    }

    @Test
    void compruebaTutorYMateria() throws Exception {
        mvc.perform(post("/api/v1/sesiones").with(user(estudiante)).contentType(MediaType.APPLICATION_JSON)
                        .content(datos(estudiante, fecha, 60))).andExpect(status().isBadRequest());
        String sinTutor = mapper.writeValueAsString(new SesionRequest(Long.MAX_VALUE, materia.getId(), fecha, 60, Modalidad.VIRTUAL));
        mvc.perform(post("/api/v1/sesiones").with(user(estudiante)).contentType(MediaType.APPLICATION_JSON)
                        .content(sinTutor)).andExpect(status().isNotFound());
        String sinMateria = mapper.writeValueAsString(new SesionRequest(tutor.getId(), Long.MAX_VALUE, fecha, 60, Modalidad.VIRTUAL));
        mvc.perform(post("/api/v1/sesiones").with(user(estudiante)).contentType(MediaType.APPLICATION_JSON)
                        .content(sinMateria)).andExpect(status().isNotFound());
    }

    @Test
    void protegeSesionesAjenasYRestringeRoles() throws Exception {
        long id = solicitar(estudiante, tutor, fecha);
        mvc.perform(get("/api/v1/sesiones/" + id).with(user(otroTutor))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/sesiones").with(user(otroTutor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(patch("/api/v1/sesiones/" + id + "/confirmar").with(user(otroTutor))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/sesiones/" + id + "/rechazar").with(user(otroTutor))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/sesiones/" + id + "/confirmar").with(user(estudiante))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/sesiones").with(user(tutor)).contentType(MediaType.APPLICATION_JSON)
                .content(datos(tutor, fecha, 60))).andExpect(status().isForbidden());
        Usuario coordinador = usuarios.findByEmailIgnoreCase("coordinador@edumentor.test").orElseThrow();
        mvc.perform(get("/api/v1/sesiones/" + id).with(user(coordinador))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/sesiones").with(user(coordinador))).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void permiteRechazarPeroNoVolverAResponder() throws Exception {
        long id = solicitar(estudiante, tutor, fecha);
        mvc.perform(patch("/api/v1/sesiones/" + id + "/rechazar").with(user(tutor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("REJECTED"));
        mvc.perform(patch("/api/v1/sesiones/" + id + "/confirmar").with(user(tutor)))
                .andExpect(status().isConflict());
    }

    @Test
    void exigeTokenYDevuelve404ParaSesionInexistente() throws Exception {
        mvc.perform(get("/api/v1/sesiones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/sesiones/999999").with(user(estudiante)))
                .andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
