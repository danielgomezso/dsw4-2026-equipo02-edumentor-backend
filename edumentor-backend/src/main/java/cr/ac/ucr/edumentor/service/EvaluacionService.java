package cr.ac.ucr.edumentor.service;

import cr.ac.ucr.edumentor.dto.*;
import cr.ac.ucr.edumentor.entity.*;
import cr.ac.ucr.edumentor.repository.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class EvaluacionService {
    private final EvaluacionRepository evaluaciones;
    private final SesionRepository sesiones;
    private final UsuarioRepository usuarios;
    private final SesionService sesionService;

    public EvaluacionService(EvaluacionRepository evaluaciones, SesionRepository sesiones,
                             UsuarioRepository usuarios, SesionService sesionService) {
        this.evaluaciones = evaluaciones;
        this.sesiones = sesiones;
        this.usuarios = usuarios;
        this.sesionService = sesionService;
    }

    @Transactional
    public EvaluacionResponse create(Long sesionId, Usuario estudiante, EvaluacionRequest request) {
        Long tutorId = sesiones.findTutorIdById(sesionId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la sesion"));
        usuarios.findByIdForUpdate(tutorId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el tutor"));
        SesionTutoria sesion = sesiones.findById(sesionId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la sesion"));
        if (!sesion.getEstudiante().getId().equals(estudiante.getId())) {
            throw new AccessDeniedException("Solo el estudiante de la sesion puede evaluarla");
        }
        if (sesion.getEstado() != EstadoSesion.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede evaluar una sesion finalizada y sin evaluacion previa");
        }
        EvaluacionTutoria evaluacion = new EvaluacionTutoria();
        evaluacion.setSesion(sesion);
        evaluacion.setCalificacionEstrellas(request.calificacionEstrellas());
        evaluacion.setComentario(request.comentario() == null ? null : request.comentario().trim());
        evaluacion.setFechaEvaluacion(LocalDateTime.now(ZoneId.of("America/Costa_Rica")));
        evaluaciones.saveAndFlush(evaluacion);
        sesion.setEstado(EstadoSesion.REVIEWED);
        sesiones.saveAndFlush(sesion);
        return EvaluacionResponse.from(evaluacion);
    }

    public EvaluacionResponse getBySesion(Long sesionId, Usuario usuario) {
        sesionService.getById(sesionId, usuario);
        return evaluaciones.findBySesionId(sesionId).map(EvaluacionResponse::from).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "La sesion todavia no tiene evaluacion"));
    }

    public CalificacionTutorResponse getCalificacion(Long tutorId) {
        Usuario tutor = usuarios.findById(tutorId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el tutor"));
        if (tutor.getRol() != Rol.TUTOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario no es tutor");
        }
        return evaluaciones.calificacionPorTutor(tutorId);
    }
}
