package cr.ac.ucr.edumentor.service;

import cr.ac.ucr.edumentor.entity.*;
import cr.ac.ucr.edumentor.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SesionServiceTest {
    @Mock SesionRepository sesiones;
    @Mock UsuarioRepository usuarios;
    @Mock MateriaRepository materias;
    @Mock DisponibilidadRepository disponibilidades;
    @InjectMocks SesionService service;

    final LocalDateTime fecha = LocalDate.now(ZoneId.of("America/Costa_Rica")).plusDays(7).atTime(10, 0);
    Usuario tutor;
    Usuario estudiante;
    SesionTutoria solicitud;

    @BeforeEach
    void setup() {
        tutor = mock(Usuario.class);
        estudiante = mock(Usuario.class);
        when(tutor.getId()).thenReturn(2L);
        lenient().when(estudiante.getId()).thenReturn(1L);
        solicitud = sesion(fecha, 60);
        when(sesiones.findById(10L)).thenReturn(Optional.of(solicitud));
        when(usuarios.findByIdForUpdate(2L)).thenReturn(Optional.of(tutor));
        when(usuarios.findByIdForUpdate(1L)).thenReturn(Optional.of(estudiante));
        lenient().when(sesiones.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        DisponibilidadTutor bloque = new DisponibilidadTutor();
        bloque.setDiaSemana(fecha.getDayOfWeek().getValue());
        bloque.setHoraInicio(LocalTime.of(8, 0));
        bloque.setHoraFin(LocalTime.of(14, 0));
        bloque.setEstaActivo(true);
        lenient().when(disponibilidades.findByTutorIdOrderByDiaSemanaAscHoraInicioAsc(2L)).thenReturn(List.of(bloque));
    }

    private SesionTutoria sesion(LocalDateTime inicio, int minutos) {
        SesionTutoria sesion = new SesionTutoria();
        sesion.setTutor(tutor);
        sesion.setEstudiante(estudiante);
        sesion.setMateria(new Materia());
        sesion.setFechaHora(inicio);
        sesion.setDuracionMinutos(minutos);
        sesion.setEstado(EstadoSesion.REQUESTED);
        sesion.setModalidad(Modalidad.VIRTUAL);
        return sesion;
    }

    @Test
    void confirmaReservaSinTraslape() {
        when(sesiones.findPosiblesTraslapes(eq(2L), eq(1L), eq(fecha.plusHours(1)), anyList())).thenReturn(List.of());
        assertEquals(EstadoSesion.CONFIRMED, service.confirmar(10L, tutor).estado());
        verify(sesiones).saveAndFlush(solicitud);
        var orden = inOrder(usuarios);
        orden.verify(usuarios).findByIdForUpdate(2L);
        orden.verify(usuarios).findByIdForUpdate(1L);
    }

    @Test
    void rechazaReservaConTraslape() {
        when(sesiones.findPosiblesTraslapes(anyLong(), anyLong(), any(), anyList()))
                .thenReturn(List.of(sesion(fecha.minusMinutes(30), 90)));
        assertEquals(EstadoSesion.REJECTED, service.confirmar(10L, tutor).estado());
        verify(sesiones).saveAndFlush(solicitud);
    }

    @Test
    void permiteSesionesConsecutivas() {
        when(sesiones.findPosiblesTraslapes(anyLong(), anyLong(), any(), anyList()))
                .thenReturn(List.of(sesion(fecha.minusHours(1), 60)));
        assertEquals(EstadoSesion.CONFIRMED, service.confirmar(10L, tutor).estado());
    }

    @Test
    void rechazaSesionQueContieneOtra() {
        when(sesiones.findPosiblesTraslapes(anyLong(), anyLong(), any(), anyList()))
                .thenReturn(List.of(sesion(fecha.plusMinutes(15), 15)));
        assertEquals(EstadoSesion.REJECTED, service.confirmar(10L, tutor).estado());
    }

    @Test
    void vuelveAComprobarDisponibilidadAlConfirmar() {
        when(disponibilidades.findByTutorIdOrderByDiaSemanaAscHoraInicioAsc(2L)).thenReturn(List.of());
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.confirmar(10L, tutor));
        assertEquals(400, error.getStatusCode().value());
        verify(sesiones, never()).saveAndFlush(any());
    }

    @Test
    void noConfirmaSolicitudesDelPasado() {
        solicitud.setFechaHora(fecha.minusYears(1));
        assertThrows(ResponseStatusException.class, () -> service.confirmar(10L, tutor));
        verify(sesiones, never()).saveAndFlush(any());
    }
}
