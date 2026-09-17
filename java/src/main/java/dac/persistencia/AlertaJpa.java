package dac.persistencia;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Una alerta señala un par contratista-funcionario. No expresa culpabilidad
 * ni puntaje: es una hipótesis a verificar (R-4).
 *
 * <p>La evidencia se mapea como N:M contra {@code contrato} a través de la
 * tabla {@code alerta_contrato}: una alerta se sustenta en varios contratos
 * y un contrato puede sustentar varias alertas. Sin evidencia la alerta no
 * vale nada (HU-02).
 */
@Entity
@Table(name = "alerta",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_alerta_par",
           columnNames = {"id_contratista", "id_funcionario", "tipo"}))
public class AlertaJpa {

    /** Hoy existe una sola señal (ck_alerta_tipo). Fraccionamiento es HU-03. */
    public static final String REINCIDENCIA = "REINCIDENCIA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Long id;

    @Column(nullable = false, length = 30)
    private String tipo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_contratista")
    private ContratistaJpa contratista;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_funcionario")
    private FuncionarioJpa funcionario;

    /** Lo pone la base de datos (DEFAULT now()), no la aplicación. */
    @Column(name = "generada_en", insertable = false, updatable = false)
    private OffsetDateTime generadaEn;

    @ManyToMany
    @JoinTable(name = "alerta_contrato",
               joinColumns = @JoinColumn(name = "id_alerta"),
               inverseJoinColumns = @JoinColumn(name = "id_contrato"))
    private Set<ContratoJpa> evidencia = new LinkedHashSet<>();

    protected AlertaJpa() {}

    public AlertaJpa(ContratistaJpa contratista, FuncionarioJpa funcionario) {
        this.tipo = REINCIDENCIA;
        this.contratista = contratista;
        this.funcionario = funcionario;
    }

    public Long getId() { return id; }
    public String getTipo() { return tipo; }
    public ContratistaJpa getContratista() { return contratista; }
    public FuncionarioJpa getFuncionario() { return funcionario; }
    public OffsetDateTime getGeneradaEn() { return generadaEn; }
    public Set<ContratoJpa> getEvidencia() { return evidencia; }
}
