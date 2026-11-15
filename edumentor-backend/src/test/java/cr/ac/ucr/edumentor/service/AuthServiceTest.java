package cr.ac.ucr.edumentor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cr.ac.ucr.edumentor.dto.AuthResponse;
import cr.ac.ucr.edumentor.dto.LoginRequest;
import cr.ac.ucr.edumentor.dto.RegisterRequest;
import cr.ac.ucr.edumentor.entity.Rol;
import cr.ac.ucr.edumentor.entity.Usuario;
import cr.ac.ucr.edumentor.repository.UsuarioRepository;
import cr.ac.ucr.edumentor.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UsuarioRepository usuarioRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    AuthenticationManager authenticationManager;
    @Mock
    JwtService jwtService;
    @InjectMocks
    AuthService authService;

    private RegisterRequest buildRequest(Rol rol) {
        return new RegisterRequest(" b12345 ", " Ana@Correo.com ", "Secreta123", "Ana", "Mora", rol);
    }

    @Test
    void registerHashesPasswordNormalizesFieldsAndReturnsToken() {
        when(usuarioRepository.existsByEmailIgnoreCase("ana@correo.com")).thenReturn(false);
        when(usuarioRepository.existsByCarneIgnoreCase("B12345")).thenReturn(false);
        when(passwordEncoder.encode("Secreta123")).thenReturn("$2a$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 7L);
            return u;
        });
        when(jwtService.generateToken(any())).thenReturn("jwt");

        AuthResponse response = authService.register(buildRequest(Rol.TUTOR));

        ArgumentCaptor<Usuario> saved = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(saved.capture());
        assertEquals("$2a$hash", saved.getValue().getPasswordHash());
        assertEquals("ana@correo.com", saved.getValue().getEmail());
        assertEquals("B12345", saved.getValue().getCarne());
        assertEquals("jwt", response.token());
        assertEquals(Rol.TUTOR, response.user().rol());
    }

    @Test
    void registerRejectsDuplicateEmailWithConflict() {
        when(usuarioRepository.existsByEmailIgnoreCase("ana@correo.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(buildRequest(Rol.ESTUDIANTE)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registerRejectsDuplicateCarneWithConflict() {
        when(usuarioRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(usuarioRepository.existsByCarneIgnoreCase("B12345")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(buildRequest(Rol.ESTUDIANTE)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registerRejectsCoordinatorSelfRegistration() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(buildRequest(Rol.COORDINADOR)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void loginWithValidCredentialsReturnsToken() {
        Usuario user = new Usuario("E1", "ana@correo.com", "$2a$hash", "Ana", "Mora", Rol.ESTUDIANTE);
        ReflectionTestUtils.setField(user, "id", 3L);
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(user)).thenReturn("jwt");

        AuthResponse response = authService.login(new LoginRequest("ANA@correo.com", "Secreta123"));

        assertEquals("jwt", response.token());
        assertEquals(3L, response.user().id());
        verify(usuarioRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void loginWithInvalidCredentialsPropagatesBadCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("ana@correo.com", "wrong")));
        verify(jwtService, never()).generateToken(any());
    }
}
