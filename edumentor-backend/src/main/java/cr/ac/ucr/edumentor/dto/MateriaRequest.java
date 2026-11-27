package cr.ac.ucr.edumentor.dto;

import jakarta.validation.constraints.*;

public record MateriaRequest(
        @NotBlank(message = "El codigo es obligatorio")
        @Size(max = 20, message = "El codigo admite maximo 20 caracteres")
        String codigoMateria,
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre admite maximo 150 caracteres")
        String nombreMateria,
        @NotBlank(message = "La carrera es obligatoria")
        @Size(max = 150, message = "La carrera admite maximo 150 caracteres")
        String carrera,
        @NotNull(message = "Los creditos son obligatorios")
        @Positive(message = "Los creditos deben ser mayores que cero")
        Integer creditos) {
}
