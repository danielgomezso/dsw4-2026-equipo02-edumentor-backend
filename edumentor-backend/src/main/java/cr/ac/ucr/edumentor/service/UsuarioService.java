package cr.ac.ucr.edumentor.service;

import cr.ac.ucr.edumentor.dto.UsuarioResponse;
import cr.ac.ucr.edumentor.repository.UsuarioRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public UsuarioResponse getById(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el usuario con id " + id));
    }

    public List<UsuarioResponse> list() {
        return usuarioRepository.findAll(Sort.by("apellido", "nombre"))
                .stream()
                .map(UsuarioResponse::from)
                .toList();
    }
}
