package cr.ac.ucr.edumentor.dto;

import cr.ac.ucr.edumentor.entity.Materia;

public record MateriaResponse(Long id, String codigoMateria, String nombreMateria,
                              String carrera, Integer creditos) {
    public static MateriaResponse from(Materia materia) {
        return new MateriaResponse(materia.getId(), materia.getCodigoMateria(),
                materia.getNombreMateria(), materia.getCarrera(), materia.getCreditos());
    }
}
