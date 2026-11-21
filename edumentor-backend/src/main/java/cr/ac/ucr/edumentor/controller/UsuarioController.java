package cr.ac.ucr.edumentor.controller;

import cr.ac.ucr.edumentor.dto.UsuarioResponse;
import cr.ac.ucr.edumentor.entity.Usuario;
import cr.ac.ucr.edumentor.service.UsuarioService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal Usuario user) {
        return UsuarioResponse.from(user);
    }

    @GetMapping
    @PreAuthorize("hasRole('COORDINADOR')")
    public List<UsuarioResponse> list() {
        return usuarioService.list();
    }

    // Can check other users info if coordinator or if user checking is the owner
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('COORDINADOR') or #id == principal.id")
    public UsuarioResponse getById(@PathVariable Long id) {
        return usuarioService.getById(id);
    }
}
