package cr.ac.ucr.edumentor.repository;

import cr.ac.ucr.edumentor.entity.EstadoSesion;
import cr.ac.ucr.edumentor.entity.SesionTutoria;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SesionRepository extends JpaRepository<SesionTutoria, Long> {
    List<SesionTutoria> findByEstudianteIdOrTutorIdOrderByFechaHoraAsc(Long estudianteId, Long tutorId);

    @Query("""
            select s from SesionTutoria s
            where s.estado in :estados
              and (s.tutor.id = :tutorId or s.estudiante.id = :estudianteId)
              and s.fechaHora < :fin
            """)
    List<SesionTutoria> findPosiblesTraslapes(@Param("tutorId") Long tutorId,
            @Param("estudianteId") Long estudianteId, @Param("fin") LocalDateTime fin,
            @Param("estados") List<EstadoSesion> estados);
}
