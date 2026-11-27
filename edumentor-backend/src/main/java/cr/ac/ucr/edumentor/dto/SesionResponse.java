package cr.ac.ucr.edumentor.dto;

import cr.ac.ucr.edumentor.entity.EstadoSesion;
import cr.ac.ucr.edumentor.entity.Modalidad;
import cr.ac.ucr.edumentor.entity.SesionTutoria;
import java.time.LocalDateTime;

public record SesionResponse(Long id, Long estudianteId, Long tutorId, Long materiaId,
                             LocalDateTime fechaHora, Integer duracionMinutos,
                             EstadoSesion estado, Modalidad modalidad, String resumen) {
    public static SesionResponse from(SesionTutoria sesion) {
        return new SesionResponse(sesion.getId(), sesion.getEstudiante().getId(),
                sesion.getTutor().getId(), sesion.getMateria().getId(), sesion.getFechaHora(),
                sesion.getDuracionMinutos(), sesion.getEstado(), sesion.getModalidad(), sesion.getResumen());
    }
}
