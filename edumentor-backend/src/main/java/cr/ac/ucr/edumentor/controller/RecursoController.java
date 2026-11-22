package cr.ac.ucr.edumentor.controller;

import cr.ac.ucr.edumentor.dto.RecursoRequest;
import cr.ac.ucr.edumentor.dto.RecursoResponse;
import cr.ac.ucr.edumentor.entity.Usuario;
import cr.ac.ucr.edumentor.service.RecursoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sesiones/{sesionId}/recursos")
public class RecursoController {
    private final RecursoService service;

    public RecursoController(RecursoService service) {
        this.service = service;
    }

    @GetMapping
    public List<RecursoResponse> list(@PathVariable Long sesionId, @AuthenticationPrincipal Usuario usuario) {
        return service.list(sesionId, usuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'TUTOR')")
    public RecursoResponse create(@PathVariable Long sesionId, @AuthenticationPrincipal Usuario usuario,
                                  @Valid @RequestBody RecursoRequest request) {
        return service.create(sesionId, usuario, request);
    }
}
