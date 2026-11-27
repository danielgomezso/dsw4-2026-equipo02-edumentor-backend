package cr.ac.ucr.edumentor.dto;

import cr.ac.ucr.edumentor.entity.DisponibilidadTutor;
import java.time.LocalTime;

public record DisponibilidadResponse(Long id, Long tutorId, Integer diaSemana,
                                     LocalTime horaInicio, LocalTime horaFin, Boolean estaActivo) {
    public static DisponibilidadResponse from(DisponibilidadTutor disponibilidad) {
        return new DisponibilidadResponse(disponibilidad.getId(), disponibilidad.getTutor().getId(),
                disponibilidad.getDiaSemana(), disponibilidad.getHoraInicio(),
                disponibilidad.getHoraFin(), disponibilidad.getEstaActivo());
    }
}
