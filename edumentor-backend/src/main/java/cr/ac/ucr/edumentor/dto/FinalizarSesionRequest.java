package cr.ac.ucr.edumentor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FinalizarSesionRequest(
        @NotBlank(message = "El resumen es obligatorio")
        @Size(max = 2000, message = "El resumen admite maximo 2000 caracteres")
        String resumen) {
}
