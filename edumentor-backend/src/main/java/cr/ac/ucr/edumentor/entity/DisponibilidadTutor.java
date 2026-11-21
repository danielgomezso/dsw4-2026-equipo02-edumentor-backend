package cr.ac.ucr.edumentor.entity;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "disponibilidad_tutor")
public class DisponibilidadTutor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Usuario tutor;

    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "esta_activo", nullable = false)
    private Boolean estaActivo;

    public DisponibilidadTutor() {}

    public Long getId() { return id; }
    public Usuario getTutor() { return tutor; }
    public Integer getDiaSemana() { return diaSemana; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public Boolean getEstaActivo() { return estaActivo; }

    public void setTutor(Usuario tutor) { this.tutor = tutor; }
    public void setDiaSemana(Integer diaSemana) { this.diaSemana = diaSemana; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public void setEstaActivo(Boolean estaActivo) { this.estaActivo = estaActivo; }
}
