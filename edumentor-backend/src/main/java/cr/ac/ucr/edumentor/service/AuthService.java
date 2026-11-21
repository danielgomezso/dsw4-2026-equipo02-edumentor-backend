package cr.ac.ucr.edumentor.service;

import cr.ac.ucr.edumentor.dto.AuthResponse;
import cr.ac.ucr.edumentor.dto.LoginRequest;
import cr.ac.ucr.edumentor.dto.RegisterRequest;
import cr.ac.ucr.edumentor.dto.UsuarioResponse;
import cr.ac.ucr.edumentor.entity.Rol;
import cr.ac.ucr.edumentor.entity.Usuario;
import cr.ac.ucr.edumentor.repository.UsuarioRepository;
import cr.ac.ucr.edumentor.security.JwtService;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.rol() == Rol.COORDINADOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo es posible registrarse como ESTUDIANTE o TUTOR");
        }
        String email = normalizeEmail(request.email());
        String carne = request.carne().trim().toUpperCase(Locale.ROOT);

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo");
        }
        if (usuarioRepository.existsByCarneIgnoreCase(carne)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese carne");
        }

        Usuario saved = usuarioRepository.save(new Usuario(
                carne, email, passwordEncoder.encode(request.password()),
                request.nombre().trim(), request.apellido().trim(), request.rol()));
        return issueToken(saved);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizeEmail(request.email()), request.password()));
        return issueToken((Usuario) auth.getPrincipal());
    }

    private AuthResponse issueToken(Usuario user) {
        return new AuthResponse(jwtService.generateToken(user), UsuarioResponse.from(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
