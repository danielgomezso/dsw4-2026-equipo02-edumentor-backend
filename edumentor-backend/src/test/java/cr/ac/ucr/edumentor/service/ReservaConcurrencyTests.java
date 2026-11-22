package cr.ac.ucr.edumentor.service;

import cr.ac.ucr.edumentor.dto.SesionRequest;
import cr.ac.ucr.edumentor.entity.*;
import cr.ac.ucr.edumentor.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
class ReservaConcurrencyTests {
    @Autowired SesionService service;
    @Autowired SesionRepository sesiones;
    @Autowired UsuarioRepository usuarios;
    @Autowired MateriaRepository materias;
    @Autowired DisponibilidadRepository disponibilidades;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void dosTutoresNoConfirmanLaMismaHoraDelEstudiante() throws Exception {
        comprobarConfirmaciones(false);
    }

    @Test
    void dosEstudiantesNoOcupanLaMismaHoraDelTutor() throws Exception {
        comprobarConfirmaciones(true);
    }

    @Test
    void cancelarYConfirmarALaVezNoRecuperaUnaSesionCancelada() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Datos datos = tx.execute(status -> preparar(true));
        Usuario estudiante = usuarios.findById(datos.usuarios().get(2)).orElseThrow();
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch iniciar = new CountDownLatch(1);
            var confirmar = executor.submit(() -> {
                assertTrue(iniciar.await(5, TimeUnit.SECONDS));
                try {
                    return service.confirmar(datos.sesion1(), datos.tutor1()).estado();
                } catch (ResponseStatusException error) {
                    assertEquals(409, error.getStatusCode().value());
                    return null;
                }
            });
            var cancelar = executor.submit(() -> {
                assertTrue(iniciar.await(5, TimeUnit.SECONDS));
                return service.cancelar(datos.sesion1(), estudiante).estado();
            });
            iniciar.countDown();
            confirmar.get(15, TimeUnit.SECONDS);
            assertEquals(EstadoSesion.CANCELLED, cancelar.get(15, TimeUnit.SECONDS));
            tx.executeWithoutResult(status -> assertEquals(EstadoSesion.CANCELLED,
                    sesiones.findById(datos.sesion1()).orElseThrow().getEstado()));
        } finally {
            tx.executeWithoutResult(status -> {
                sesiones.deleteAllByIdInBatch(List.of(datos.sesion1(), datos.sesion2()));
                disponibilidades.deleteAllByIdInBatch(datos.horarios());
                materias.deleteAllByIdInBatch(List.of(datos.materiaId()));
                usuarios.deleteAllByIdInBatch(datos.usuarios());
            });
        }
    }

    private void comprobarConfirmaciones(boolean mismoTutor) throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Datos datos = tx.execute(status -> preparar(mismoTutor));
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch preparados = new CountDownLatch(2);
            CountDownLatch iniciar = new CountDownLatch(1);
            var primera = executor.submit(() -> {
                preparados.countDown();
                assertTrue(iniciar.await(5, TimeUnit.SECONDS));
                return service.confirmar(datos.sesion1(), datos.tutor1()).estado();
            });
            var segunda = executor.submit(() -> {
                preparados.countDown();
                assertTrue(iniciar.await(5, TimeUnit.SECONDS));
                return service.confirmar(datos.sesion2(), datos.tutor2()).estado();
            });
            assertTrue(preparados.await(5, TimeUnit.SECONDS));
            iniciar.countDown();
            List<EstadoSesion> resultados = List.of(primera.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS));
            assertEquals(1, resultados.stream().filter(e -> e == EstadoSesion.CONFIRMED).count());
            assertEquals(1, resultados.stream().filter(e -> e == EstadoSesion.REJECTED).count());
            tx.executeWithoutResult(status -> {
                var guardadas = sesiones.findAllById(List.of(datos.sesion1(), datos.sesion2()));
                assertEquals(1, guardadas.stream().filter(s -> s.getEstado() == EstadoSesion.CONFIRMED).count());
            });
        } finally {
            tx.executeWithoutResult(status -> {
                sesiones.deleteAllByIdInBatch(List.of(datos.sesion1(), datos.sesion2()));
                disponibilidades.deleteAllByIdInBatch(datos.horarios());
                materias.deleteAllByIdInBatch(List.of(datos.materiaId()));
                usuarios.deleteAllByIdInBatch(datos.usuarios());
            });
        }
    }

    private Datos preparar(boolean mismoTutor) {
        String codigo = UUID.randomUUID().toString().substring(0, 8);
        Usuario tutor1 = usuario(codigo + "T1", Rol.TUTOR);
        Usuario tutor2 = usuario(codigo + "T2", Rol.TUTOR);
        Usuario estudiante1 = usuario(codigo + "E1", Rol.ESTUDIANTE);
        Usuario estudiante2 = usuario(codigo + "E2", Rol.ESTUDIANTE);
        Materia materia = new Materia();
        materia.setCodigoMateria(codigo);
        materia.setNombreMateria("Concurrencia");
        materia.setCarrera("Informatica");
        materia.setCreditos(4);
        materias.saveAndFlush(materia);
        LocalDateTime fecha = LocalDate.now(ZoneId.of("America/Costa_Rica")).plusDays(7).atTime(10, 0);
        Long horario1 = horario(tutor1, fecha);
        Long horario2 = horario(tutor2, fecha);
        Usuario segundoTutor = mismoTutor ? tutor1 : tutor2;
        Usuario segundoEstudiante = mismoTutor ? estudiante2 : estudiante1;
        Long primera = service.create(estudiante1,
                new SesionRequest(tutor1.getId(), materia.getId(), fecha, 60, Modalidad.VIRTUAL)).id();
        Long segunda = service.create(segundoEstudiante,
                new SesionRequest(segundoTutor.getId(), materia.getId(), fecha, 60, Modalidad.VIRTUAL)).id();
        return new Datos(primera, segunda, tutor1, segundoTutor, materia.getId(),
                List.of(horario1, horario2),
                List.of(tutor1.getId(), tutor2.getId(), estudiante1.getId(), estudiante2.getId()));
    }

    private Usuario usuario(String carne, Rol rol) {
        return usuarios.saveAndFlush(new Usuario(carne, carne + "@example.invalid", "NO_USAR", "Prueba", "Concurrente", rol));
    }

    private Long horario(Usuario tutor, LocalDateTime fecha) {
        DisponibilidadTutor horario = new DisponibilidadTutor();
        horario.setTutor(tutor);
        horario.setDiaSemana(fecha.getDayOfWeek().getValue());
        horario.setHoraInicio(LocalTime.of(9, 0));
        horario.setHoraFin(LocalTime.of(13, 0));
        horario.setEstaActivo(true);
        return disponibilidades.saveAndFlush(horario).getId();
    }

    private record Datos(Long sesion1, Long sesion2, Usuario tutor1, Usuario tutor2,
                         Long materiaId, List<Long> horarios, List<Long> usuarios) {}
}
