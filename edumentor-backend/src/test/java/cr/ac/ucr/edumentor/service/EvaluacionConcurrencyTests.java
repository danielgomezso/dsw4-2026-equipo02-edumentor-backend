package cr.ac.ucr.edumentor.service;

import cr.ac.ucr.edumentor.dto.EvaluacionRequest;
import cr.ac.ucr.edumentor.entity.*;
import cr.ac.ucr.edumentor.repository.*;
import java.time.LocalDateTime;
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
class EvaluacionConcurrencyTests {
    @Autowired EvaluacionService service;
    @Autowired EvaluacionRepository evaluaciones;
    @Autowired SesionRepository sesiones;
    @Autowired UsuarioRepository usuarios;
    @Autowired MateriaRepository materias;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void dosEnviosSimultaneosGuardanSoloUnaEvaluacion() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Datos datos = tx.execute(status -> preparar());
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch preparados = new CountDownLatch(2);
            CountDownLatch iniciar = new CountDownLatch(1);
            var primero = executor.submit(() -> {
                preparados.countDown();
                assertTrue(iniciar.await(5, TimeUnit.SECONDS));
                return evaluar(datos);
            });
            var segundo = executor.submit(() -> {
                preparados.countDown();
                assertTrue(iniciar.await(5, TimeUnit.SECONDS));
                return evaluar(datos);
            });
            assertTrue(preparados.await(5, TimeUnit.SECONDS));
            iniciar.countDown();
            var resultados = List.of(primero.get(15, TimeUnit.SECONDS), segundo.get(15, TimeUnit.SECONDS));
            assertEquals(1, resultados.stream().filter(codigo -> codigo == 201).count());
            assertEquals(1, resultados.stream().filter(codigo -> codigo == 409).count());
            tx.executeWithoutResult(status -> {
                assertEquals(EstadoSesion.REVIEWED, sesiones.findById(datos.sesionId()).orElseThrow().getEstado());
                var calificacion = service.getCalificacion(datos.tutor().getId());
                assertEquals(1, calificacion.totalEvaluaciones());
                assertEquals(4.0, calificacion.promedio());
            });
        } finally {
            tx.executeWithoutResult(status -> {
                evaluaciones.findBySesionId(datos.sesionId()).ifPresent(e -> evaluaciones.deleteAllByIdInBatch(List.of(e.getId())));
                sesiones.deleteAllByIdInBatch(List.of(datos.sesionId()));
                materias.deleteAllByIdInBatch(List.of(datos.materiaId()));
                usuarios.deleteAllByIdInBatch(List.of(datos.estudiante().getId(), datos.tutor().getId()));
            });
        }
    }

    private int evaluar(Datos datos) {
        try {
            service.create(datos.sesionId(), datos.estudiante(), new EvaluacionRequest(4, "Util"));
            return 201;
        } catch (ResponseStatusException error) {
            return error.getStatusCode().value();
        }
    }

    private Datos preparar() {
        String codigo = UUID.randomUUID().toString().substring(0, 8);
        Usuario tutor = usuarios.saveAndFlush(new Usuario(codigo + "T", codigo + "t@example.invalid", "NO_USAR", "Prueba", "Tutor", Rol.TUTOR));
        Usuario estudiante = usuarios.saveAndFlush(new Usuario(codigo + "E", codigo + "e@example.invalid", "NO_USAR", "Prueba", "Estudiante", Rol.ESTUDIANTE));
        Materia materia = new Materia();
        materia.setCodigoMateria(codigo);
        materia.setNombreMateria("Evaluacion concurrente");
        materia.setCarrera("Informatica");
        materia.setCreditos(4);
        materias.saveAndFlush(materia);
        SesionTutoria sesion = new SesionTutoria();
        sesion.setTutor(tutor);
        sesion.setEstudiante(estudiante);
        sesion.setMateria(materia);
        sesion.setFechaHora(LocalDateTime.of(2020, 6, 10, 10, 0));
        sesion.setDuracionMinutos(60);
        sesion.setEstado(EstadoSesion.COMPLETED);
        sesion.setModalidad(Modalidad.VIRTUAL);
        sesiones.saveAndFlush(sesion);
        return new Datos(sesion.getId(), estudiante, tutor, materia.getId());
    }

    private record Datos(Long sesionId, Usuario estudiante, Usuario tutor, Long materiaId) {}
}
