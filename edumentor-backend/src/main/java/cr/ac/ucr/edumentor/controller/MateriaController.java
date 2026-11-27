package cr.ac.ucr.edumentor.controller;

import cr.ac.ucr.edumentor.dto.MateriaRequest;
import cr.ac.ucr.edumentor.dto.MateriaResponse;
import cr.ac.ucr.edumentor.service.MateriaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/materias")
public class MateriaController {
    private final MateriaService service;

    public MateriaController(MateriaService service) {
        this.service = service;
    }

    @GetMapping
    public List<MateriaResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public MateriaResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDINADOR')")
    public ResponseEntity<MateriaResponse> create(@Valid @RequestBody MateriaRequest request) {
        MateriaResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/materias/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COORDINADOR')")
    public MateriaResponse update(@PathVariable Long id, @Valid @RequestBody MateriaRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDINADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
