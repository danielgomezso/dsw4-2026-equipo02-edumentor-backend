package cr.ac.ucr.edumentor.service;

import cr.ac.ucr.edumentor.dto.RecursoRequest;
import cr.ac.ucr.edumentor.dto.RecursoResponse;
import cr.ac.ucr.edumentor.entity.RecursoEstudio;
import cr.ac.ucr.edumentor.entity.Usuario;
import cr.ac.ucr.edumentor.repository.RecursoRepository;
import cr.ac.ucr.edumentor.repository.SesionRepository;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class RecursoService {
    private final RecursoRepository recursos;
    private final SesionRepository sesiones;
    private final SesionService sesionService;

    public RecursoService(RecursoRepository recursos, SesionRepository sesiones, SesionService sesionService) {
        this.recursos = recursos;
        this.sesiones = sesiones;
        this.sesionService = sesionService;
    }

    public List<RecursoResponse> list(Long sesionId, Usuario usuario) {
        sesionService.getById(sesionId, usuario);
        return recursos.findBySesionIdOrderByIdAsc(sesionId).stream().map(RecursoResponse::from).toList();
    }

    @Transactional
    public RecursoResponse create(Long sesionId, Usuario usuario, RecursoRequest request) {
        sesionService.getById(sesionId, usuario);
        String enlace = request.urlRecurso().trim();
        try {
            URI uri = URI.create(enlace);
            if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace debe ser una URL HTTP o HTTPS valida");
        }
        RecursoEstudio recurso = new RecursoEstudio();
        recurso.setSesion(sesiones.getReferenceById(sesionId));
        recurso.setTitulo(request.titulo().trim());
        recurso.setUrlRecurso(enlace);
        recurso.setTipoArchivo(request.tipoArchivo().trim());
        return RecursoResponse.from(recursos.saveAndFlush(recurso));
    }
}
