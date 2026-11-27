package cr.ac.ucr.edumentor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecursoRequest(
        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 200, message = "El titulo admite maximo 200 caracteres") String titulo,
        @NotBlank(message = "El enlace es obligatorio")
        @Size(max = 2048, message = "El enlace admite maximo 2048 caracteres") String urlRecurso,
        @NotBlank(message = "El tipo de archivo es obligatorio")
        @Size(max = 50, message = "El tipo admite maximo 50 caracteres") String tipoArchivo) {
}
