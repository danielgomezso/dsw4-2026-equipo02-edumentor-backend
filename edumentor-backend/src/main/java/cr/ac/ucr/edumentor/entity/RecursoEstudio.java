package cr.ac.ucr.edumentor.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "recurso_estudio")
public class RecursoEstudio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_id", nullable = false)
    private SesionTutoria sesion;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(name = "url_recurso", nullable = false, length = 2048)
    private String urlRecurso;

    @Column(name = "tipo_archivo", nullable = false, length = 50)
    private String tipoArchivo;

    public RecursoEstudio() {}

    public Long getId() { return id; }
    public SesionTutoria getSesion() { return sesion; }
    public String getTitulo() { return titulo; }
    public String getUrlRecurso() { return urlRecurso; }
    public String getTipoArchivo() { return tipoArchivo; }

    public void setSesion(SesionTutoria sesion) { this.sesion = sesion; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setUrlRecurso(String urlRecurso) { this.urlRecurso = urlRecurso; }
    public void setTipoArchivo(String tipoArchivo) { this.tipoArchivo = tipoArchivo; }
}
