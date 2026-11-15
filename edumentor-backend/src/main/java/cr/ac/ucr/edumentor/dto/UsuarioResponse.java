package cr.ac.ucr.edumentor.dto;

import cr.ac.ucr.edumentor.entity.Rol;
import cr.ac.ucr.edumentor.entity.Usuario;

public record UsuarioResponse(
        Long id,
        String carne,
        String email,
        String nombre,
        String apellido,
        Rol rol) {
    public static UsuarioResponse from(Usuario user) {
        return new UsuarioResponse(
                user.getId(), user.getCarne(), user.getEmail(),
                user.getNombre(), user.getApellido(), user.getRol());
    }
}
