package dac.persistencia;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contrato",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_contrato_clave_natural",
           columnNames = {"id_entidad", "numero_contrato"}))
public class ContratoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contrato")
    private Long id;

    @Column(name = "numero_contrato", nullable = false)
    private String numeroContrato;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_entidad")
    private EntidadJpa entidad;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_contratista")
    private ContratistaJpa contratista;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_funcionario")
    private FuncionarioJpa funcionario;

    /** Admite null: una fila sin monto no se descarta (R-6). */
    @Column(precision = 15, scale = 2)
    private BigDecimal monto;

    /** Admite null: una fila sin fecha no se descarta (R-6). */
    @Column
    private LocalDate fecha;

    protected ContratoJpa() {}

    public ContratoJpa(String numeroContrato, EntidadJpa entidad,
                       ContratistaJpa contratista, FuncionarioJpa funcionario,
                       BigDecimal monto, LocalDate fecha) {
        this.numeroContrato = numeroContrato;
        this.entidad = entidad;
        this.contratista = contratista;
        this.funcionario = funcionario;
        this.monto = monto;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public String getNumeroContrato() { return numeroContrato; }
    public EntidadJpa getEntidad() { return entidad; }
    public ContratistaJpa getContratista() { return contratista; }
    public FuncionarioJpa getFuncionario() { return funcionario; }
    public BigDecimal getMonto() { return monto; }
    public LocalDate getFecha() { return fecha; }
}
