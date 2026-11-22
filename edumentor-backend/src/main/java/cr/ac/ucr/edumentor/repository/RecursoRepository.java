package cr.ac.ucr.edumentor.repository;

import cr.ac.ucr.edumentor.entity.RecursoEstudio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecursoRepository extends JpaRepository<RecursoEstudio, Long> {
    List<RecursoEstudio> findBySesionIdOrderByIdAsc(Long sesionId);
}
