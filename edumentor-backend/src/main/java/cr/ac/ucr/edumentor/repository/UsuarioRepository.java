package cr.ac.ucr.edumentor.repository;

import cr.ac.ucr.edumentor.entity.Usuario;
import java.util.Optional;
import java.util.List;
import cr.ac.ucr.edumentor.entity.Rol;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> findByIdForUpdate(@Param("id") Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCarneIgnoreCase(String carne);

    List<Usuario> findByRolOrderByApellidoAscNombreAsc(Rol rol);
}
