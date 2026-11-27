package cr.ac.ucr.edumentor.dto;

import cr.ac.ucr.edumentor.entity.Usuario;

public record TutorResponse(Long id, String nombre, String apellido) {
    public static TutorResponse from(Usuario tutor) {
        return new TutorResponse(tutor.getId(), tutor.getNombre(), tutor.getApellido());
    }
}
