package cr.ac.ucr.edumentor.dto;

import jakarta.validation.constraints.*;
import java.time.LocalTime;

public record DisponibilidadRequest(
        @NotNull(message = "El dia es obligatorio")
        @Min(value = 1, message = "El dia debe estar entre 1 y 7")
        @Max(value = 7, message = "El dia debe estar entre 1 y 7")
        Integer diaSemana,
        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,
        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin,
        @NotNull(message = "El estado activo es obligatorio")
        Boolean estaActivo) {
}
