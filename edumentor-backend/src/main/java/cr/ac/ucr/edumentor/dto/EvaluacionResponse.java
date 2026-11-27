package cr.ac.ucr.edumentor.dto;

import cr.ac.ucr.edumentor.entity.EvaluacionTutoria;
import java.time.LocalDateTime;

public record EvaluacionResponse(Long id, Long sesionId, Integer calificacionEstrellas,
                                 String comentario, LocalDateTime fechaEvaluacion) {
    public static EvaluacionResponse from(EvaluacionTutoria evaluacion) {
        return new EvaluacionResponse(evaluacion.getId(), evaluacion.getSesion().getId(),
                evaluacion.getCalificacionEstrellas(), evaluacion.getComentario(), evaluacion.getFechaEvaluacion());
    }
}
