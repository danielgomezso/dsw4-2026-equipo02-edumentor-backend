package cr.ac.ucr.edumentor.repository;

import cr.ac.ucr.edumentor.entity.Materia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MateriaRepository extends JpaRepository<Materia, Long> {
    boolean existsByCodigoMateriaIgnoreCaseAndIdNot(String codigoMateria, Long id);
}
