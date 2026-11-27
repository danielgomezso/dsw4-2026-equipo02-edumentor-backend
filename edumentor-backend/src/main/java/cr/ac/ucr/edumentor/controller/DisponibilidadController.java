package cr.ac.ucr.edumentor.controller;

import cr.ac.ucr.edumentor.dto.DisponibilidadRequest;
import cr.ac.ucr.edumentor.dto.DisponibilidadResponse;
import cr.ac.ucr.edumentor.entity.Usuario;
import cr.ac.ucr.edumentor.service.DisponibilidadService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/disponibilidades")
public class DisponibilidadController {
    private final DisponibilidadService service;

    public DisponibilidadController(DisponibilidadService service) {
        this.service = service;
    }

    @GetMapping
    public List<DisponibilidadResponse> list(@RequestParam Long tutorId) {
        return service.list(tutorId);
    }

    @GetMapping("/{id}")
    public DisponibilidadResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<DisponibilidadResponse> create(@AuthenticationPrincipal Usuario tutor,
            @Valid @RequestBody DisponibilidadRequest request) {
        DisponibilidadResponse response = service.create(tutor, request);
        return ResponseEntity.created(URI.create("/api/v1/disponibilidades/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TUTOR')")
    public DisponibilidadResponse update(@PathVariable Long id, @AuthenticationPrincipal Usuario tutor,
            @Valid @RequestBody DisponibilidadRequest request) {
        return service.update(id, tutor, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Usuario tutor) {
        service.delete(id, tutor);
        return ResponseEntity.noContent().build();
    }
}
