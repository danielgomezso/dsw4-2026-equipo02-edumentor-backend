package cr.ac.ucr.edumentor.security;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class AuthSecurityIntegrationTests {

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String loginToken(String email, String password) throws Exception {
        String json = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }

    @Test
    void validLoginReturnsTokenAndPublicUser() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"tutor@edumentor.test\",\"password\":\"Tutor123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.rol").value("TUTOR"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void invalidLoginReturns401ProblemDetail() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"tutor@edumentor.test\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Credenciales o token invalidos"))
                .andExpect(jsonPath("$.instance").value("/api/v1/auth/login"));
    }

    @Test
    void missingTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciales o token invalidos"));
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciales o token invalidos"));
    }

    @Test
    void validTokenReturnsProfile() throws Exception {
        String token = loginToken("estudiante@edumentor.test", "Estudiante123!");
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("estudiante@edumentor.test"))
                .andExpect(jsonPath("$.rol").value("ESTUDIANTE"));
    }

    @Test
    void onlyCoordinatorCanListAllUsers() throws Exception {
        mvc.perform(get("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + loginToken("tutor@edumentor.test", "Tutor123!")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Su rol no tiene permiso para este recurso"));
        mvc.perform(get("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + loginToken("coordinador@edumentor.test", "Coordinador123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
    }

    @Test
    void userCanReadOwnRecordButNotAnotherUsersRecord() throws Exception {
        String studentToken = "Bearer " + loginToken("estudiante@edumentor.test", "Estudiante123!");
        mvc.perform(get("/api/v1/usuarios/1").header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("estudiante@edumentor.test"));
        mvc.perform(get("/api/v1/usuarios/2").header("Authorization", studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerCreatesAccountAndAllowsLogin() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carne":"b99999","email":"Nuevo@Ucr.ac.cr","password":"Segura123",
                                 "nombre":"Nuevo","apellido":"Usuario","rol":"ESTUDIANTE"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.carne").value("B99999"))
                .andExpect(jsonPath("$.user.email").value("nuevo@ucr.ac.cr"))
                .andExpect(jsonPath("$.token").isNotEmpty());

        loginToken("nuevo@ucr.ac.cr", "Segura123");
    }

    @Test
    void registerDuplicateEmailReturns409ProblemDetail() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carne":"X1","email":"tutor@edumentor.test","password":"Segura123",
                                 "nombre":"A","apellido":"B","rol":"TUTOR"}"""))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Ya existe una cuenta con ese correo"));
    }

    @Test
    void registerInvalidInputReturns400WithValidationDetail() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carne\":\"\",\"email\":\"no-es-correo\",\"password\":\"123\",\"rol\":\"TUTOR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void registerAsCoordinatorReturns400() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carne":"C9","email":"jefe@ucr.ac.cr","password":"Segura123",
                                 "nombre":"A","apellido":"B","rol":"COORDINADOR"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Solo es posible registrarse como ESTUDIANTE o TUTOR"));
    }

    @Test
    void corsAllowsAngularOrigin() throws Exception {
        mvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }
}
