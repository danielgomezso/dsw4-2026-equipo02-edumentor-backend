package cr.ac.ucr.edumentor.service;

import cr.ac.ucr.edumentor.dto.DisponibilidadRequest;
import cr.ac.ucr.edumentor.dto.DisponibilidadResponse;
import cr.ac.ucr.edumentor.entity.DisponibilidadTutor;
import cr.ac.ucr.edumentor.entity.Usuario;
import cr.ac.ucr.edumentor.repository.DisponibilidadRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class DisponibilidadService {
    private final DisponibilidadRepository repository;

    public DisponibilidadService(DisponibilidadRepository repository) {
        this.repository = repository;
    }

    public List<DisponibilidadResponse> list(Long tutorId) {
        return repository.findByTutorIdOrderByDiaSemanaAscHoraInicioAsc(tutorId).stream()
                .map(DisponibilidadResponse::from).toList();
    }

    public DisponibilidadResponse getById(Long id) {
        return DisponibilidadResponse.from(buscar(id));
    }

    @Transactional
    public DisponibilidadResponse create(Usuario tutor, DisponibilidadRequest request) {
        DisponibilidadTutor disponibilidad = new DisponibilidadTutor();
        disponibilidad.setTutor(tutor);
        asignarDatos(disponibilidad, request);
        return DisponibilidadResponse.from(repository.saveAndFlush(disponibilidad));
    }

    @Transactional
    public DisponibilidadResponse update(Long id, Usuario tutor, DisponibilidadRequest request) {
        DisponibilidadTutor disponibilidad = buscarPropia(id, tutor);
        asignarDatos(disponibilidad, request);
        return DisponibilidadResponse.from(repository.saveAndFlush(disponibilidad));
    }

    @Transactional
    public void delete(Long id, Usuario tutor) {
        repository.delete(buscarPropia(id, tutor));
        repository.flush();
    }

    private DisponibilidadTutor buscar(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la disponibilidad"));
    }

    private DisponibilidadTutor buscarPropia(Long id, Usuario tutor) {
        DisponibilidadTutor disponibilidad = buscar(id);
        if (!disponibilidad.getTutor().getId().equals(tutor.getId())) {
            throw new AccessDeniedException("Solo puede modificar su propia disponibilidad");
        }
        return disponibilidad;
    }

    private void asignarDatos(DisponibilidadTutor disponibilidad, DisponibilidadRequest request) {
        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La hora de fin debe ser posterior a la hora de inicio");
        }
        disponibilidad.setDiaSemana(request.diaSemana());
        disponibilidad.setHoraInicio(request.horaInicio());
        disponibilidad.setHoraFin(request.horaFin());
        disponibilidad.setEstaActivo(request.estaActivo());
    }
}
