package dac.persistencia;

import jakarta.persistence.*;

@Entity
@Table(name = "contratista")
public class ContratistaJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contratista")
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    protected ContratistaJpa() {}

    public ContratistaJpa(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
}
