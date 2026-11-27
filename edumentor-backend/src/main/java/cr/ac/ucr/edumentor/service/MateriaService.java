package cr.ac.ucr.edumentor.service;

import cr.ac.ucr.edumentor.dto.MateriaRequest;
import cr.ac.ucr.edumentor.dto.MateriaResponse;
import cr.ac.ucr.edumentor.entity.Materia;
import cr.ac.ucr.edumentor.repository.MateriaRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class MateriaService {
    private final MateriaRepository repository;

    public MateriaService(MateriaRepository repository) {
        this.repository = repository;
    }

    public List<MateriaResponse> list() {
        return repository.findAll(Sort.by("codigoMateria")).stream()
                .map(MateriaResponse::from).toList();
    }

    public MateriaResponse getById(Long id) {
        return MateriaResponse.from(buscar(id));
    }

    @Transactional
    public MateriaResponse create(MateriaRequest request) {
        Materia materia = new Materia();
        asignarDatos(materia, request);
        return MateriaResponse.from(repository.saveAndFlush(materia));
    }

    @Transactional
    public MateriaResponse update(Long id, MateriaRequest request) {
        Materia materia = buscar(id);
        asignarDatos(materia, request);
        return MateriaResponse.from(repository.saveAndFlush(materia));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(buscar(id));
        repository.flush();
    }

    private Materia buscar(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la materia"));
    }

    private void asignarDatos(Materia materia, MateriaRequest request) {
        String codigo = request.codigoMateria().trim().toUpperCase(Locale.ROOT);
        Long id = materia.getId() == null ? 0L : materia.getId();
        if (repository.existsByCodigoMateriaIgnoreCaseAndIdNot(codigo, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una materia con ese codigo");
        }
        materia.setCodigoMateria(codigo);
        materia.setNombreMateria(request.nombreMateria().trim());
        materia.setCarrera(request.carrera().trim());
        materia.setCreditos(request.creditos());
    }
}
