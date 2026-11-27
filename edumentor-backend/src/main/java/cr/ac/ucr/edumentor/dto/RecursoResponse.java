package cr.ac.ucr.edumentor.dto;

import cr.ac.ucr.edumentor.entity.RecursoEstudio;

public record RecursoResponse(Long id, Long sesionId, String titulo, String urlRecurso, String tipoArchivo) {
    public static RecursoResponse from(RecursoEstudio recurso) {
        return new RecursoResponse(recurso.getId(), recurso.getSesion().getId(), recurso.getTitulo(),
                recurso.getUrlRecurso(), recurso.getTipoArchivo());
    }
}
