package dac.aplicacion;

import dac.dominio.*;
import dac.persistencia.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Orquesta la rebanada completa:
 * CSV → dominio → persistencia → detección → respuesta.
 */
@Service
public class ContratoServicio {

    private final CargadorDeContratos cargador;
    private final DetectorDeReincidencias detector;
    private final EntidadRepo entidadRepo;
    private final ContratistaRepo contratistaRepo;
    private final FuncionarioRepo funcionarioRepo;
    private final ContratoRepo contratoRepo;

    public ContratoServicio(CargadorDeContratos cargador,
                            DetectorDeReincidencias detector,
                            EntidadRepo entidadRepo,
                            ContratistaRepo contratistaRepo,
                            FuncionarioRepo funcionarioRepo,
                            ContratoRepo contratoRepo) {
        this.cargador = cargador;
        this.detector = detector;
        this.entidadRepo = entidadRepo;
        this.contratistaRepo = contratistaRepo;
        this.funcionarioRepo = funcionarioRepo;
        this.contratoRepo = contratoRepo;
    }

    /**
     * Carga el CSV, persiste los contratos nuevos (idempotencia — HU-07)
     * y devuelve las alertas detectadas sobre el histórico completo.
     */
    @Transactional
    public ResultadoServicio cargarYDetectar(InputStream csv) {
        // 1. Dominio: leer CSV y deduplicar dentro de la carga
        ResultadoCarga carga = cargador.cargarDesde(csv);

        // 2. Persistencia: guardar solo los contratos nuevos (ON CONFLICT = idempotencia)
        int nuevos = 0;
        for (Contrato c : carga.contratos()) {
            EntidadJpa entidad = entidadRepo.findByNombre(c.entidad())
                    .orElseGet(() -> entidadRepo.save(new EntidadJpa(c.entidad())));

            ContratistaJpa contratista = contratistaRepo.findByNombre(c.contratista())
                    .orElseGet(() -> contratistaRepo.save(new ContratistaJpa(c.contratista())));

            FuncionarioJpa funcionario = funcionarioRepo.findByNombre(c.funcionario())
                    .orElseGet(() -> funcionarioRepo.save(new FuncionarioJpa(c.funcionario())));

            boolean existe = contratoRepo
                    .findByNumeroContratoAndEntidad(c.numeroContrato(), entidad)
                    .isPresent();

            if (!existe) {
                contratoRepo.save(new ContratoJpa(
                        c.numeroContrato(), entidad, contratista, funcionario,
                        aMonto(c.monto()), aFecha(c.fecha())));
                nuevos++;
            }
        }

        // 3. Detección sobre el histórico completo en BD (HU-07)
        List<Contrato> historico = contratoRepo.findAllConFetch().stream()
                .map(j -> Contrato.de(
                        j.getNumeroContrato(),
                        j.getEntidad().getNombre(),
                        j.getContratista().getNombre(),
                        j.getFuncionario().getNombre(),
                        j.getMonto() == null ? "" : j.getMonto().toPlainString(),
                        j.getFecha() == null ? "" : j.getFecha().toString()))
                .toList();

        List<Alerta> alertas = detector.detectar(historico);

        return new ResultadoServicio(nuevos, historico.size(), carga.duplicadosIgnorados(),
                carga.filasDescartadas(), alertas);
    }

    /**
     * El dominio trata monto y fecha como texto (R-8) y R-6 dice que estar
     * vacíos no descarta la fila: la señal de reincidencia no los usa. Aquí
     * el vacío se convierte en NULL en lugar de romper la carga completa.
     */
    private static BigDecimal aMonto(String monto) {
        return monto == null || monto.isBlank() ? null : new BigDecimal(monto);
    }

    private static LocalDate aFecha(String fecha) {
        return fecha == null || fecha.isBlank() ? null : LocalDate.parse(fecha);
    }
}
