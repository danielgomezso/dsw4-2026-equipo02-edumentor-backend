package cr.ac.ucr.edumentor.repository;

import cr.ac.ucr.edumentor.entity.EvaluacionTutoria;
import cr.ac.ucr.edumentor.dto.CalificacionTutorResponse;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvaluacionRepository extends JpaRepository<EvaluacionTutoria, Long> {
    Optional<EvaluacionTutoria> findBySesionId(Long sesionId);

    @Query("""
            select new cr.ac.ucr.edumentor.dto.CalificacionTutorResponse(
                :tutorId, avg(cast(e.calificacionEstrellas as double)), count(e))
            from EvaluacionTutoria e where e.sesion.tutor.id = :tutorId
            """)
    CalificacionTutorResponse calificacionPorTutor(@Param("tutorId") Long tutorId);
}
