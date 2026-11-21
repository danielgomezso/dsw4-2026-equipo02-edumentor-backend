package cr.ac.ucr.edumentor.dto;

import cr.ac.ucr.edumentor.entity.Modalidad;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public record SesionRequest(
        @NotNull(message = "El tutor es obligatorio") @Positive Long tutorId,
        @NotNull(message = "La materia es obligatoria") @Positive Long materiaId,
        @NotNull(message = "La fecha y hora son obligatorias") LocalDateTime fechaHora,
        @NotNull(message = "La duracion es obligatoria")
        @Positive(message = "La duracion debe ser mayor que cero") Integer duracionMinutos,
        @NotNull(message = "La modalidad es obligatoria") Modalidad modalidad) {
}
