package cr.ac.ucr.edumentor.exception;

import cr.ac.ucr.edumentor.controller.MateriaController;
import cr.ac.ucr.edumentor.service.MateriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {
    MateriaService service;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(MateriaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new MateriaController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void errorInesperadoDevuelve500SinDetallesInternos() throws Exception {
        when(service.list()).thenThrow(new IllegalStateException("Detalle interno de prueba"));
        mvc.perform(get("/api/v1/materias"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").isNotEmpty()).andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Ocurrio un error interno en el servidor"))
                .andExpect(jsonPath("$.instance").value("/api/v1/materias"));
    }

    @Test
    void conservaEl404DelServicio() throws Exception {
        when(service.getById(99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la materia"));
        mvc.perform(get("/api/v1/materias/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe la materia"));
    }

    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        mvc.perform(post("/api/v1/materias").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/v1/materias"));
    }

    @Test
    void identificadorNoNumericoDevuelve400() throws Exception {
        mvc.perform(get("/api/v1/materias/texto")).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void metodoNoPermitidoConserva405YAllow() throws Exception {
        mvc.perform(patch("/api/v1/materias/1")).andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow")).andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void conservaValidacionDeCampos() throws Exception {
        mvc.perform(post("/api/v1/materias").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").isNotEmpty());
    }
}
