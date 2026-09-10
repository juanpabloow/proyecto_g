package dac.persistencia;

import jakarta.persistence.*;

@Entity
@Table(name = "funcionario")
public class FuncionarioJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_funcionario")
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    protected FuncionarioJpa() {}

    public FuncionarioJpa(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
}
