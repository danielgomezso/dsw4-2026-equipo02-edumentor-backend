package cr.ac.ucr.edumentor.dto;

import cr.ac.ucr.edumentor.entity.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Self-registration payload; only ESTUDIANTE and TUTOR are allowed in AuthService. */
public record RegisterRequest(
        @NotBlank(message = "El carne es obligatorio")
        @Size(max = 20, message = "El carne admite maximo 20 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9]+$", message = "El carne solo admite letras y numeros")
        String carne,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 254, message = "El correo admite maximo 254 caracteres")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
        String password,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite maximo 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido admite maximo 100 caracteres")
        String apellido,

        @NotNull(message = "El rol es obligatorio")
        Rol rol
) {
}
