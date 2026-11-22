package cr.ac.ucr.edumentor.dto;

import jakarta.validation.constraints.*;

public record EvaluacionRequest(
        @NotNull(message = "La calificacion es obligatoria")
        @Min(value = 1, message = "La calificacion debe estar entre 1 y 5")
        @Max(value = 5, message = "La calificacion debe estar entre 1 y 5") Integer calificacionEstrellas,
        @Size(max = 2000, message = "El comentario admite maximo 2000 caracteres") String comentario) {
}
