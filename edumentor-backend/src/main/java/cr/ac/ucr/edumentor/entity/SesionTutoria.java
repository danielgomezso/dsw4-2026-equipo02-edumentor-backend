package cr.ac.ucr.edumentor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sesion_tutoria")
public class SesionTutoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Usuario estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Usuario tutor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSesion estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Modalidad modalidad;

    @Column(length = 2000)
    private String resumen;

    public SesionTutoria() {}

    public Long getId() { return id; }
    public Usuario getEstudiante() { return estudiante; }
    public Usuario getTutor() { return tutor; }
    public Materia getMateria() { return materia; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public Integer getDuracionMinutos() { return duracionMinutos; }
    public EstadoSesion getEstado() { return estado; }
    public Modalidad getModalidad() { return modalidad; }
    public String getResumen() { return resumen; }

    public void setEstudiante(Usuario estudiante) { this.estudiante = estudiante; }
    public void setTutor(Usuario tutor) { this.tutor = tutor; }
    public void setMateria(Materia materia) { this.materia = materia; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
    public void setEstado(EstadoSesion estado) { this.estado = estado; }
    public void setModalidad(Modalidad modalidad) { this.modalidad = modalidad; }
    public void setResumen(String resumen) { this.resumen = resumen; }
}
