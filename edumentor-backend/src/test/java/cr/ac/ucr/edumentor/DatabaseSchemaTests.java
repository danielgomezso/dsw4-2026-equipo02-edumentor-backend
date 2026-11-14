package cr.ac.ucr.edumentor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class DatabaseSchemaTests {
    @Autowired JdbcTemplate jdbc;

    private long usuario(String carne, String rol) {
        jdbc.update("INSERT INTO dbo.usuario(carne,email,password_hash,nombre,apellido,rol) VALUES (?,?,?,'Prueba','Temporal',?)",
                carne, carne + "@example.invalid", "NO_ES_UN_HASH_DE_ACCESO", rol);
        return jdbc.queryForObject("SELECT id FROM dbo.usuario WHERE carne=?", Long.class, carne);
    }

    private long sesion() {
        long tutor = usuario("T001", "TUTOR");
        long estudiante = usuario("E001", "ESTUDIANTE");
        jdbc.update("INSERT INTO dbo.materia(codigo_materia,nombre_materia,carrera,creditos) VALUES ('IF0009','Software IV','Informatica',4)");
        long materia = jdbc.queryForObject("SELECT id FROM dbo.materia WHERE codigo_materia='IF0009'", Long.class);
        jdbc.update("INSERT INTO dbo.sesion_tutoria(tutor_id,estudiante_id,materia_id,fecha_hora,duracion_minutos,modalidad) VALUES (?,?,?,'2026-11-20 10:00:00',60,'VIRTUAL')", tutor, estudiante, materia);
        return jdbc.queryForObject("SELECT id FROM dbo.sesion_tutoria WHERE tutor_id=?", Long.class, tutor);
    }

    @Test void guardaRelacionesYEstadoInicial() {
        long id = sesion();
        jdbc.update("INSERT INTO dbo.recurso_estudio(sesion_id,titulo,url_recurso,tipo_archivo) VALUES (?,'Guia','https://example.invalid/guia','PDF')", id);
        assertEquals("REQUESTED", jdbc.queryForObject("SELECT estado FROM dbo.sesion_tutoria WHERE id=?", String.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dbo.recurso_estudio r JOIN dbo.sesion_tutoria s ON s.id=r.sesion_id WHERE s.id=?", Integer.class, id));
    }
    @Test void rechazaRolInvalido() {
        assertThrows(DataIntegrityViolationException.class, () -> usuario("X001", "ADMIN"));
    }
    @Test void rechazaCorreoDuplicado() {
        usuario("T001", "TUTOR");
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("INSERT INTO dbo.usuario(carne,email,password_hash,nombre,apellido,rol) VALUES ('OTRO','T001@example.invalid','INVALIDO','A','B','TUTOR')"));
    }
    @Test void rechazaHorarioInvertido() {
        long tutor = usuario("T001", "TUTOR");
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("INSERT INTO dbo.disponibilidad_tutor(tutor_id,dia_semana,hora_inicio,hora_fin) VALUES (?,1,'11:00:00','10:00:00')", tutor));
    }
    @Test void rechazaRecursoSinSesion() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("INSERT INTO dbo.recurso_estudio(sesion_id,titulo,url_recurso,tipo_archivo) VALUES (-1,'Guia','https://example.invalid','PDF')"));
    }
    @Test void rechazaEstrellasFueraDeRango() {
        long id = sesion();
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("INSERT INTO dbo.evaluacion_tutoria(sesion_id,calificacion_estrellas,fecha_evaluacion) VALUES (?,6,CURRENT_TIMESTAMP)", id));
    }
    @Test void permiteUnaEvaluacionPorSesion() {
        long id = sesion();
        jdbc.update("INSERT INTO dbo.evaluacion_tutoria(sesion_id,calificacion_estrellas,fecha_evaluacion) VALUES (?,5,CURRENT_TIMESTAMP)", id);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("INSERT INTO dbo.evaluacion_tutoria(sesion_id,calificacion_estrellas,fecha_evaluacion) VALUES (?,4,CURRENT_TIMESTAMP)", id));
    }
}
