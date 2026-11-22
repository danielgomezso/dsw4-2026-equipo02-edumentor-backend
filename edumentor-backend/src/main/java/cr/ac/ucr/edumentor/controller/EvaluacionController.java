package cr.ac.ucr.edumentor.controller;

import cr.ac.ucr.edumentor.dto.*;
import cr.ac.ucr.edumentor.entity.Usuario;
import cr.ac.ucr.edumentor.service.EvaluacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class EvaluacionController {
    private final EvaluacionService service;

    public EvaluacionController(EvaluacionService service) {
        this.service = service;
    }

    @PostMapping("/sesiones/{sesionId}/evaluacion")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public EvaluacionResponse create(@PathVariable Long sesionId, @AuthenticationPrincipal Usuario estudiante,
                                     @Valid @RequestBody EvaluacionRequest request) {
        return service.create(sesionId, estudiante, request);
    }

    @GetMapping("/sesiones/{sesionId}/evaluacion")
    public EvaluacionResponse getBySesion(@PathVariable Long sesionId, @AuthenticationPrincipal Usuario usuario) {
        return service.getBySesion(sesionId, usuario);
    }

    @GetMapping("/tutores/{tutorId}/calificacion")
    public CalificacionTutorResponse getCalificacion(@PathVariable Long tutorId) {
        return service.getCalificacion(tutorId);
    }
}
