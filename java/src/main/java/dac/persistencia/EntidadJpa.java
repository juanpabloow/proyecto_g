package dac.persistencia;

import jakarta.persistence.*;

@Entity
@Table(name = "entidad")
public class EntidadJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entidad")
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    protected EntidadJpa() {}

    public EntidadJpa(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
}
