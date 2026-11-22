package cr.ac.ucr.edumentor.service;

import cr.ac.ucr.edumentor.dto.FinalizarSesionRequest;
import cr.ac.ucr.edumentor.entity.*;
import cr.ac.ucr.edumentor.repository.*;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CicloSesionServiceTest {
    @Mock SesionRepository sesiones;
    @Mock UsuarioRepository usuarios;
    @Mock MateriaRepository materias;
    @Mock DisponibilidadRepository disponibilidades;
    @Spy @InjectMocks SesionService service;

    private static final LocalDateTime AHORA = LocalDateTime.of(2030, 6, 10, 10, 0);
    Usuario estudiante;
    Usuario tutor;
    SesionTutoria sesion;

    @BeforeEach
    void setup() {
        estudiante = mock(Usuario.class);
        tutor = mock(Usuario.class);
        lenient().when(estudiante.getId()).thenReturn(1L);
        lenient().when(tutor.getId()).thenReturn(2L);
        sesion = new SesionTutoria();
        sesion.setEstudiante(estudiante);
        sesion.setTutor(tutor);
        sesion.setMateria(new Materia());
        sesion.setFechaHora(AHORA.plusHours(2));
        sesion.setDuracionMinutos(60);
        sesion.setModalidad(Modalidad.VIRTUAL);
        sesion.setEstado(EstadoSesion.CONFIRMED);
        when(sesiones.findTutorIdById(10L)).thenReturn(Optional.of(2L));
        when(usuarios.findByIdForUpdate(2L)).thenReturn(Optional.of(tutor));
        when(sesiones.findById(10L)).thenReturn(Optional.of(sesion));
        lenient().when(sesiones.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doReturn(AHORA).when(service).ahora();
    }

    @Test
    void permiteCancelarExactamenteDosHorasAntes() {
        assertEquals(EstadoSesion.CANCELLED, service.cancelar(10L, estudiante).estado());
        var orden = inOrder(sesiones, usuarios);
        orden.verify(sesiones).findTutorIdById(10L);
        orden.verify(usuarios).findByIdForUpdate(2L);
        orden.verify(sesiones).findById(10L);
    }

    @Test
    void rechazaCancelarConMenosDeDosHoras() {
        sesion.setFechaHora(AHORA.plusHours(2).minusNanos(1));
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.cancelar(10L, estudiante));
        assertEquals(400, error.getStatusCode().value());
        verify(sesiones, never()).saveAndFlush(any());
    }

    @Test
    void tutorPuedeCancelarConfirmadaConAnticipacion() {
        sesion.setFechaHora(AHORA.plusHours(3));
        assertEquals(EstadoSesion.CANCELLED, service.cancelar(10L, tutor).estado());
    }

    @Test
    void estudiantePuedeCancelarSolicitudPendiente() {
        sesion.setEstado(EstadoSesion.REQUESTED);
        assertEquals(EstadoSesion.CANCELLED, service.cancelar(10L, estudiante).estado());
    }

    @Test
    void tutorNoCancelaSolicitudPendiente() {
        sesion.setEstado(EstadoSesion.REQUESTED);
        assertThrows(AccessDeniedException.class, () -> service.cancelar(10L, tutor));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSesion.class, names = {"REQUESTED", "CONFIRMED"}, mode = EnumSource.Mode.EXCLUDE)
    void noCancelaOtrosEstados(EstadoSesion estado) {
        sesion.setEstado(estado);
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.cancelar(10L, estudiante));
        assertEquals(409, error.getStatusCode().value());
    }

    @Test
    void noIniciaAntesDeLaHoraPactada() {
        sesion.setFechaHora(AHORA.plusNanos(1));
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.iniciar(10L, tutor));
        assertEquals(400, error.getStatusCode().value());
    }

    @Test
    void iniciaExactamenteALaHoraPactada() {
        sesion.setFechaHora(AHORA);
        assertEquals(EstadoSesion.IN_SESSION, service.iniciar(10L, tutor).estado());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSesion.class, names = "CONFIRMED", mode = EnumSource.Mode.EXCLUDE)
    void noIniciaOtrosEstados(EstadoSesion estado) {
        sesion.setEstado(estado);
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.iniciar(10L, tutor));
        assertEquals(409, error.getStatusCode().value());
    }

    @Test
    void finalizaYGuardaResumen() {
        sesion.setEstado(EstadoSesion.IN_SESSION);
        var respuesta = service.finalizar(10L, tutor, new FinalizarSesionRequest("  Repaso de funciones  "));
        assertEquals(EstadoSesion.COMPLETED, respuesta.estado());
        assertEquals("Repaso de funciones", respuesta.resumen());
        verify(sesiones).saveAndFlush(sesion);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSesion.class, names = "IN_SESSION", mode = EnumSource.Mode.EXCLUDE)
    void noFinalizaOtrosEstados(EstadoSesion estado) {
        sesion.setEstado(estado);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.finalizar(10L, tutor, new FinalizarSesionRequest("Resumen")));
        assertEquals(409, error.getStatusCode().value());
    }
}
