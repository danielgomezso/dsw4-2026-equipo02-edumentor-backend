package cr.ac.ucr.edumentor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class MateriaIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc;

    private static final String DATOS = """
            {"codigoMateria":" if0009 ","nombreMateria":"Software IV","carrera":"Informatica","creditos":4}
            """;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private long crear() throws Exception {
        String json = mvc.perform(post("/api/v1/materias").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoMateria").value("IF0009"))
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get("id").asLong();
    }

    @Test
    @WithMockUser(roles = "COORDINADOR")
    void coordinadorPuedeCrearConsultarEditarYEliminar() throws Exception {
        long id = crear();
        mvc.perform(get("/api/v1/materias/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.creditos").value(4));
        mvc.perform(get("/api/v1/materias")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoMateria").value("IF0009"));
        mvc.perform(put("/api/v1/materias/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(DATOS.replace("Software IV", "Software actualizado")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombreMateria").value("Software actualizado"));
        mvc.perform(delete("/api/v1/materias/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/materias/" + id)).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @WithMockUser(roles = "COORDINADOR")
    void rechazaCodigoDuplicado() throws Exception {
        crear();
        mvc.perform(post("/api/v1/materias").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.instance").value("/api/v1/materias"));
    }

    @Test
    @WithMockUser(roles = "COORDINADOR")
    void rechazaCamposInvalidos() throws Exception {
        mvc.perform(post("/api/v1/materias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigoMateria\":\"\",\"creditos\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void estudianteConsultaPeroNoModifica() throws Exception {
        mvc.perform(get("/api/v1/materias")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/materias").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/materias/1").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/materias/1")).andExpect(status().isForbidden());
    }

    @Test
    void exigeAutenticacion() throws Exception {
        mvc.perform(get("/api/v1/materias")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "COORDINADOR")
    void noEliminaMateriaConSesiones() throws Exception {
        long id = crear();
        jdbc.update("INSERT INTO dbo.sesion_tutoria(estudiante_id,tutor_id,materia_id,fecha_hora,duracion_minutos,modalidad) VALUES (1,2,?,'2026-11-25 10:00:00',60,'VIRTUAL')", id);
        mvc.perform(delete("/api/v1/materias/" + id)).andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }
}
