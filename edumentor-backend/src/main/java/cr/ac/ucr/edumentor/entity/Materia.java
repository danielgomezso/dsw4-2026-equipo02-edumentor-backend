package cr.ac.ucr.edumentor.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "materia")
public class Materia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_materia", nullable = false, unique = true, length = 20)
    private String codigoMateria;

    @Column(name = "nombre_materia", nullable = false, length = 150)
    private String nombreMateria;

    @Column(nullable = false, length = 150)
    private String carrera;

    @Column(nullable = false)
    private Integer creditos;

    public Materia() {}

    public Long getId() { return id; }
    public String getCodigoMateria() { return codigoMateria; }
    public String getNombreMateria() { return nombreMateria; }
    public String getCarrera() { return carrera; }
    public Integer getCreditos() { return creditos; }

    public void setCodigoMateria(String codigoMateria) { this.codigoMateria = codigoMateria; }
    public void setNombreMateria(String nombreMateria) { this.nombreMateria = nombreMateria; }
    public void setCarrera(String carrera) { this.carrera = carrera; }
    public void setCreditos(Integer creditos) { this.creditos = creditos; }
}
