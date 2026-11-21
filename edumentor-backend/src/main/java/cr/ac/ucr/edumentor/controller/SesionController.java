package cr.ac.ucr.edumentor.controller;

import cr.ac.ucr.edumentor.dto.SesionRequest;
import cr.ac.ucr.edumentor.dto.SesionResponse;
import cr.ac.ucr.edumentor.entity.Usuario;
import cr.ac.ucr.edumentor.service.SesionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sesiones")
public class SesionController {
    private final SesionService service;

    public SesionController(SesionService service) {
        this.service = service;
    }

    @GetMapping
    public List<SesionResponse> list(@AuthenticationPrincipal Usuario usuario) {
        return service.list(usuario);
    }

    @GetMapping("/{id}")
    public SesionResponse getById(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return service.getById(id, usuario);
    }

    @PostMapping
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<SesionResponse> create(@AuthenticationPrincipal Usuario estudiante,
                                                @Valid @RequestBody SesionRequest request) {
        SesionResponse response = service.create(estudiante, request);
        return ResponseEntity.created(URI.create("/api/v1/sesiones/" + response.id())).body(response);
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasRole('TUTOR')")
    public SesionResponse confirmar(@PathVariable Long id, @AuthenticationPrincipal Usuario tutor) {
        return service.confirmar(id, tutor);
    }

    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('TUTOR')")
    public SesionResponse rechazar(@PathVariable Long id, @AuthenticationPrincipal Usuario tutor) {
        return service.rechazar(id, tutor);
    }
}
