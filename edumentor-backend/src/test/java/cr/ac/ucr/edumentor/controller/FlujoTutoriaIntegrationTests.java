package cr.ac.ucr.edumentor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cr.ac.ucr.edumentor.entity.*;
import cr.ac.ucr.edumentor.repository.*;
import cr.ac.ucr.edumentor.service.SesionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.hibernate.SessionFactory;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class FlujoTutoriaIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepository usuarios;
    @Autowired MateriaRepository materias;
    @Autowired SesionRepository sesiones;
    @Autowired SesionService sesionService;
    @Autowired EntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String login(String correo, String clave) throws Exception {
        String json = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", correo, "password", clave))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readTree(json).get("token").asText();
    }

    private long crear(String ruta, String token, Object datos) throws Exception {
        String json = mvc.perform(post(ruta).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(datos)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get("id").asLong();
    }

    @Test
    void flujoCompletoConTokensReales() throws Exception {
        String estudiante = login("estudiante@edumentor.test", "Estudiante123!");
        String tutor = login("tutor@edumentor.test", "Tutor123!");
        String coordinador = login("coordinador@edumentor.test", "Coordinador123!");
        mvc.perform(get("/api/v1/usuarios/tutores").header("Authorization", estudiante))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].email").doesNotExist()).andExpect(jsonPath("$[0].carne").doesNotExist())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist()).andExpect(jsonPath("$.length()").value(1));
        long materiaId = crear("/api/v1/materias", coordinador,
                Map.of("codigoMateria", "FLUJO01", "nombreMateria", "Software", "carrera", "Informatica", "creditos", 4));
        LocalDateTime fecha = LocalDate.now(ZoneId.of("America/Costa_Rica")).plusDays(1).atTime(10, 0);
        crear("/api/v1/disponibilidades", tutor,
                Map.of("diaSemana", fecha.getDayOfWeek().getValue(), "horaInicio", "09:00", "horaFin", "12:00", "estaActivo", true));
        long sesionId = crear("/api/v1/sesiones", estudiante,
                Map.of("tutorId", 2, "materiaId", materiaId, "fechaHora", fecha.toString(), "duracionMinutos", 60, "modalidad", "VIRTUAL"));
        String ruta = "/api/v1/sesiones/" + sesionId;
        mvc.perform(patch(ruta + "/confirmar").header("Authorization", tutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMED"));
        crear(ruta + "/recursos", tutor,
                Map.of("titulo", "Guia", "urlRecurso", "https://example.com/guia.pdf", "tipoArchivo", "PDF"));
        mvc.perform(get(ruta + "/recursos").header("Authorization", estudiante))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

        // Simula el paso del tiempo; los limites horarios tienen pruebas independientes.
        SesionTutoria sesion = sesiones.findById(sesionId).orElseThrow();
        sesion.setFechaHora(LocalDateTime.now(ZoneId.of("America/Costa_Rica")).minusMinutes(1));
        sesiones.saveAndFlush(sesion);
        mvc.perform(patch(ruta + "/iniciar").header("Authorization", tutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("IN_SESSION"));
        mvc.perform(patch(ruta + "/finalizar").header("Authorization", tutor).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resumen\":\"Repaso de funciones\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("COMPLETED"));
        crear(ruta + "/evaluacion", estudiante, Map.of("calificacionEstrellas", 5, "comentario", "Muy util"));
        mvc.perform(get(ruta).header("Authorization", estudiante)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("REVIEWED"));
        mvc.perform(get("/api/v1/tutores/2/calificacion").header("Authorization", estudiante))
                .andExpect(status().isOk()).andExpect(jsonPath("$.promedio").value(5.0))
                .andExpect(jsonPath("$.totalEvaluaciones").value(1));
    }

    @Test
    void catalogoDeTutoresRequiereAutenticacion() throws Exception {
        mvc.perform(get("/api/v1/usuarios/tutores")).andExpect(status().isUnauthorized());
    }

    @Test
    void listarSesionesNoGeneraUnaConsultaPorRelacion() {
        Usuario estudiante = usuarios.findByEmailIgnoreCase("estudiante@edumentor.test").orElseThrow();
        for (int i = 0; i < 6; i++) {
            Usuario tutor = usuarios.saveAndFlush(new Usuario("N1T" + i, "n1t" + i + "@example.invalid", "NO_USAR", "Tutor", "Prueba", Rol.TUTOR));
            Materia materia = new Materia();
            materia.setCodigoMateria("N1M" + i);
            materia.setNombreMateria("Prueba");
            materia.setCarrera("Informatica");
            materia.setCreditos(4);
            materias.saveAndFlush(materia);
            SesionTutoria sesion = new SesionTutoria();
            sesion.setEstudiante(estudiante);
            sesion.setTutor(tutor);
            sesion.setMateria(materia);
            sesion.setFechaHora(LocalDateTime.of(2030, 6, 10, 10, 0));
            sesion.setDuracionMinutos(60);
            sesion.setEstado(EstadoSesion.REQUESTED);
            sesion.setModalidad(Modalidad.VIRTUAL);
            sesiones.saveAndFlush(sesion);
        }
        entityManager.clear();
        var estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean activadas = estadisticas.isStatisticsEnabled();
        estadisticas.setStatisticsEnabled(true);
        estadisticas.clear();
        try {
            assertEquals(6, sesionService.list(estudiante).size());
            assertEquals(1, estadisticas.getPrepareStatementCount());
        } finally {
            estadisticas.setStatisticsEnabled(activadas);
        }
    }
}
