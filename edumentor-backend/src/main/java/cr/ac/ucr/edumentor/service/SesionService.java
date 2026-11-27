package cr.ac.ucr.edumentor.service;

import cr.ac.ucr.edumentor.dto.SesionRequest;
import cr.ac.ucr.edumentor.dto.FinalizarSesionRequest;
import cr.ac.ucr.edumentor.dto.SesionResponse;
import cr.ac.ucr.edumentor.entity.*;
import cr.ac.ucr.edumentor.repository.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class SesionService {
    private final SesionRepository sesiones;
    private final UsuarioRepository usuarios;
    private final MateriaRepository materias;
    private final DisponibilidadRepository disponibilidades;

    public SesionService(SesionRepository sesiones, UsuarioRepository usuarios,
                         MateriaRepository materias, DisponibilidadRepository disponibilidades) {
        this.sesiones = sesiones;
        this.usuarios = usuarios;
        this.materias = materias;
        this.disponibilidades = disponibilidades;
    }

    public List<SesionResponse> list(Usuario usuario) {
        List<SesionTutoria> resultado = usuario.getRol() == Rol.COORDINADOR
                ? sesiones.findAll(Sort.by("fechaHora"))
                : sesiones.findByEstudianteIdOrTutorIdOrderByFechaHoraAsc(usuario.getId(), usuario.getId());
        return resultado.stream().map(SesionResponse::from).toList();
    }

    public SesionResponse getById(Long id, Usuario usuario) {
        SesionTutoria sesion = buscar(id);
        if (usuario.getRol() != Rol.COORDINADOR
                && !usuario.getId().equals(sesion.getEstudiante().getId())
                && !usuario.getId().equals(sesion.getTutor().getId())) {
            throw new AccessDeniedException("Solo puede consultar sus propias sesiones");
        }
        return SesionResponse.from(sesion);
    }

    @Transactional
    public SesionResponse create(Usuario estudiante, SesionRequest request) {
        Usuario tutor = usuarios.findById(request.tutorId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el tutor"));
        if (tutor.getRol() != Rol.TUTOR || tutor.getId().equals(estudiante.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar un tutor valido");
        }
        Materia materia = materias.findById(request.materiaId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la materia"));
        SesionTutoria sesion = new SesionTutoria();
        sesion.setEstudiante(estudiante);
        sesion.setTutor(tutor);
        sesion.setMateria(materia);
        sesion.setFechaHora(request.fechaHora());
        sesion.setDuracionMinutos(request.duracionMinutos());
        sesion.setModalidad(request.modalidad());
        sesion.setEstado(EstadoSesion.REQUESTED);
        validarDisponibilidad(sesion);
        if (hayTraslape(sesion)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El tutor o estudiante ya tiene una sesion en ese horario");
        }
        return SesionResponse.from(sesiones.saveAndFlush(sesion));
    }

    @Transactional
    public SesionResponse confirmar(Long id, Usuario tutor) {
        // Se bloquea primero al tutor y luego al estudiante hasta terminar la transaccion.
        bloquearUsuario(tutor.getId());
        SesionTutoria sesion = buscarSolicitudPropia(id, tutor);
        bloquearUsuario(sesion.getEstudiante().getId());
        validarDisponibilidad(sesion);
        sesion.setEstado(hayTraslape(sesion) ? EstadoSesion.REJECTED : EstadoSesion.CONFIRMED);
        return SesionResponse.from(sesiones.saveAndFlush(sesion));
    }

    @Transactional
    public SesionResponse rechazar(Long id, Usuario tutor) {
        bloquearUsuario(tutor.getId());
        SesionTutoria sesion = buscarSolicitudPropia(id, tutor);
        sesion.setEstado(EstadoSesion.REJECTED);
        return SesionResponse.from(sesiones.saveAndFlush(sesion));
    }

    @Transactional
    public SesionResponse cancelar(Long id, Usuario usuario) {
        SesionTutoria sesion = buscarParaCambiar(id);
        boolean esEstudiante = usuario.getId().equals(sesion.getEstudiante().getId());
        boolean esTutor = usuario.getId().equals(sesion.getTutor().getId());
        if (!esEstudiante && !esTutor) {
            throw new AccessDeniedException("Solo los participantes pueden cancelar la sesion");
        }
        if (sesion.getEstado() != EstadoSesion.REQUESTED && sesion.getEstado() != EstadoSesion.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden cancelar solicitudes pendientes o sesiones confirmadas");
        }
        if (sesion.getEstado() == EstadoSesion.REQUESTED && !esEstudiante) {
            throw new AccessDeniedException("El tutor debe rechazar la solicitud pendiente");
        }
        if (ahora().isAfter(sesion.getFechaHora().minusHours(2))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe cancelar con al menos dos horas de anticipacion");
        }
        sesion.setEstado(EstadoSesion.CANCELLED);
        return SesionResponse.from(sesiones.saveAndFlush(sesion));
    }

    @Transactional
    public SesionResponse iniciar(Long id, Usuario tutor) {
        SesionTutoria sesion = buscarParaCambiar(id);
        validarTutor(sesion, tutor);
        if (sesion.getEstado() != EstadoSesion.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede iniciar una sesion confirmada");
        }
        if (ahora().isBefore(sesion.getFechaHora())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sesion no puede iniciar antes de la hora pactada");
        }
        sesion.setEstado(EstadoSesion.IN_SESSION);
        return SesionResponse.from(sesiones.saveAndFlush(sesion));
    }

    @Transactional
    public SesionResponse finalizar(Long id, Usuario tutor, FinalizarSesionRequest request) {
        SesionTutoria sesion = buscarParaCambiar(id);
        validarTutor(sesion, tutor);
        if (sesion.getEstado() != EstadoSesion.IN_SESSION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede finalizar una sesion en curso");
        }
        sesion.setResumen(request.resumen().trim());
        sesion.setEstado(EstadoSesion.COMPLETED);
        return SesionResponse.from(sesiones.saveAndFlush(sesion));
    }

    private SesionTutoria buscarParaCambiar(Long id) {
        Long tutorId = sesiones.findTutorIdById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la sesion"));
        bloquearUsuario(tutorId);
        return buscar(id);
    }

    private void validarTutor(SesionTutoria sesion, Usuario tutor) {
        if (!sesion.getTutor().getId().equals(tutor.getId())) {
            throw new AccessDeniedException("Solo el tutor asignado puede realizar esta accion");
        }
    }

    LocalDateTime ahora() {
        return LocalDateTime.now(ZoneId.of("America/Costa_Rica"));
    }

    private SesionTutoria buscar(Long id) {
        return sesiones.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la sesion"));
    }

    private SesionTutoria buscarSolicitudPropia(Long id, Usuario tutor) {
        SesionTutoria sesion = buscar(id);
        if (!sesion.getTutor().getId().equals(tutor.getId())) {
            throw new AccessDeniedException("Solo el tutor asignado puede responder la solicitud");
        }
        if (sesion.getEstado() != EstadoSesion.REQUESTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede responder una solicitud pendiente");
        }
        return sesion;
    }

    private void bloquearUsuario(Long id) {
        usuarios.findByIdForUpdate(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el usuario"));
    }

    private void validarDisponibilidad(SesionTutoria sesion) {
        LocalDateTime inicio = sesion.getFechaHora();
        LocalDateTime fin = inicio.plusMinutes(sesion.getDuracionMinutos());
        if (!inicio.isAfter(ahora())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sesion debe iniciar en una fecha futura");
        }
        boolean disponible = inicio.toLocalDate().equals(fin.toLocalDate())
                && disponibilidades.findByTutorIdOrderByDiaSemanaAscHoraInicioAsc(sesion.getTutor().getId())
                .stream().anyMatch(bloque -> bloque.getEstaActivo()
                        && bloque.getDiaSemana() == inicio.getDayOfWeek().getValue()
                        && !inicio.toLocalTime().isBefore(bloque.getHoraInicio())
                        && !fin.toLocalTime().isAfter(bloque.getHoraFin()));
        if (!disponible) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sesion debe quedar dentro de un horario activo del tutor");
        }
    }

    private boolean hayTraslape(SesionTutoria sesion) {
        LocalDateTime fin = sesion.getFechaHora().plusMinutes(sesion.getDuracionMinutos());
        return sesiones.findPosiblesTraslapes(sesion.getTutor().getId(), sesion.getEstudiante().getId(), fin,
                        List.of(EstadoSesion.CONFIRMED, EstadoSesion.IN_SESSION)).stream()
                .anyMatch(otra -> sesion.getFechaHora().isBefore(otra.getFechaHora().plusMinutes(otra.getDuracionMinutos())));
    }
}
