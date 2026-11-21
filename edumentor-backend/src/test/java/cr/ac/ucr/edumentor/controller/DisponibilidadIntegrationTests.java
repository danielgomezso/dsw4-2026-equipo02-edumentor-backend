package cr.ac.ucr.edumentor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cr.ac.ucr.edumentor.entity.Rol;
import cr.ac.ucr.edumentor.entity.Usuario;
import cr.ac.ucr.edumentor.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
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
class DisponibilidadIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepository usuarios;
    MockMvc mvc;

    private static final String DATOS = """
            {"diaSemana":1,"horaInicio":"10:00","horaFin":"11:00","estaActivo":true}
            """;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private long crear() throws Exception {
        String json = mvc.perform(post("/api/v1/disponibilidades")
                .contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tutorId").value(2))
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get("id").asLong();
    }

    @Test
    @WithUserDetails("tutor@edumentor.test")
    void tutorGestionaSuDisponibilidad() throws Exception {
        long id = crear();
        mvc.perform(get("/api/v1/disponibilidades").param("tutorId", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
        mvc.perform(put("/api/v1/disponibilidades/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(DATOS.replace("true", "false")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estaActivo").value(false));
        mvc.perform(get("/api/v1/disponibilidades/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estaActivo").value(false));
        mvc.perform(put("/api/v1/disponibilidades/" + id).contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estaActivo").value(true));
        mvc.perform(delete("/api/v1/disponibilidades/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/disponibilidades/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("tutor@edumentor.test")
    void rechazaHorarioInvertidoOIgual() throws Exception {
        for (String horaFin : new String[]{"09:00", "10:00"}) {
            mvc.perform(post("/api/v1/disponibilidades").contentType(MediaType.APPLICATION_JSON)
                    .content(DATOS.replace("11:00", horaFin)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("La hora de fin debe ser posterior a la hora de inicio"));
        }
    }

    @Test
    @WithUserDetails("tutor@edumentor.test")
    void rechazaDiaInvalidoYCamposFaltantes() throws Exception {
        mvc.perform(post("/api/v1/disponibilidades").contentType(MediaType.APPLICATION_JSON)
                .content(DATOS.replace("\"diaSemana\":1", "\"diaSemana\":8")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/disponibilidades").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    @WithUserDetails("tutor@edumentor.test")
    void rechazaBloqueDuplicado() throws Exception {
        crear();
        mvc.perform(post("/api/v1/disponibilidades").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithUserDetails("tutor@edumentor.test")
    void otroTutorNoPuedeModificarNiEliminarElBloque() throws Exception {
        long id = crear();
        Usuario otro = usuarios.saveAndFlush(new Usuario("OTRO1", "otro@example.invalid", "NO_USAR",
                "Otro", "Tutor", Rol.TUTOR));
        mvc.perform(put("/api/v1/disponibilidades/" + id).with(user(otro))
                .contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/disponibilidades/" + id).with(user(otro)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("estudiante@edumentor.test")
    void estudianteConsultaPeroNoModifica() throws Exception {
        mvc.perform(get("/api/v1/disponibilidades").param("tutorId", "2")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/disponibilidades").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/disponibilidades/1").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/disponibilidades/1")).andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("tutor@edumentor.test")
    void noPermiteAsignarElBloqueAOtroTutor() throws Exception {
        mvc.perform(post("/api/v1/disponibilidades").contentType(MediaType.APPLICATION_JSON)
                .content(DATOS.replace("{", "{\"tutorId\":1,")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.tutorId").value(2));
    }

    @Test
    void exigeAutenticacion() throws Exception {
        mvc.perform(get("/api/v1/disponibilidades").param("tutorId", "2"))
                .andExpect(status().isUnauthorized());
    }
}
