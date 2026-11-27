package cr.ac.ucr.edumentor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluacion_tutoria")
public class EvaluacionTutoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_id", nullable = false, unique = true)
    private SesionTutoria sesion;

    @Column(name = "calificacion_estrellas", nullable = false)
    private Integer calificacionEstrellas;

    @Column(length = 2000)
    private String comentario;

    @Column(name = "fecha_evaluacion", nullable = false)
    private LocalDateTime fechaEvaluacion;

    public EvaluacionTutoria() {}

    public Long getId() { return id; }
    public SesionTutoria getSesion() { return sesion; }
    public Integer getCalificacionEstrellas() { return calificacionEstrellas; }
    public String getComentario() { return comentario; }
    public LocalDateTime getFechaEvaluacion() { return fechaEvaluacion; }

    public void setSesion(SesionTutoria sesion) { this.sesion = sesion; }
    public void setCalificacionEstrellas(Integer calificacionEstrellas) { this.calificacionEstrellas = calificacionEstrellas; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public void setFechaEvaluacion(LocalDateTime fechaEvaluacion) { this.fechaEvaluacion = fechaEvaluacion; }
}
