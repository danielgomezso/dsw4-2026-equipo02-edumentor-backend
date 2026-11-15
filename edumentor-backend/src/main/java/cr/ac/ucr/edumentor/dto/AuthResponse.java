package cr.ac.ucr.edumentor.dto;

public record AuthResponse(
        String token,
        UsuarioResponse user) {
}