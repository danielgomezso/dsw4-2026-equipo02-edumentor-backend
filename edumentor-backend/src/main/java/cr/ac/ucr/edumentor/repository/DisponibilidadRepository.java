package cr.ac.ucr.edumentor.repository;

import cr.ac.ucr.edumentor.entity.DisponibilidadTutor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisponibilidadRepository extends JpaRepository<DisponibilidadTutor, Long> {
    List<DisponibilidadTutor> findByTutorIdOrderByDiaSemanaAscHoraInicioAsc(Long tutorId);
}
