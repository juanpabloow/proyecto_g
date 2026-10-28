package dac.aplicacion;

import dac.dominio.*;
import dac.persistencia.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta la rebanada completa:
 * CSV → dominio → persistencia → detección → respuesta.
 */
@Service
public class ContratoServicio {

    private final CargadorDeContratos cargador;
    private final DetectorDeReincidencias detector;
    private final DetectorDeFraccionamiento detectorFraccionamiento;
    private final EntidadRepo entidadRepo;
    private final ContratistaRepo contratistaRepo;
    private final FuncionarioRepo funcionarioRepo;
    private final ContratoRepo contratoRepo;
    private final AlertaRepo alertaRepo;

    public ContratoServicio(CargadorDeContratos cargador,
                            DetectorDeReincidencias detector,
                            DetectorDeFraccionamiento detectorFraccionamiento,
                            EntidadRepo entidadRepo,
                            ContratistaRepo contratistaRepo,
                            FuncionarioRepo funcionarioRepo,
                            ContratoRepo contratoRepo,
                            AlertaRepo alertaRepo) {
        this.cargador = cargador;
        this.detector = detector;
        this.detectorFraccionamiento = detectorFraccionamiento;
        this.entidadRepo = entidadRepo;
        this.contratistaRepo = contratistaRepo;
        this.funcionarioRepo = funcionarioRepo;
        this.contratoRepo = contratoRepo;
        this.alertaRepo = alertaRepo;
    }

    /**
     * Carga el CSV, persiste los contratos nuevos (idempotencia — HU-07),
     * reporta los duplicados con datos distintos (HU-06) y devuelve las dos
     * señales calculadas sobre el histórico completo (HU-02 y HU-03).
     */
    @Transactional
    public ResultadoServicio cargarYDetectar(InputStream csv) {
        // 1. Dominio: leer CSV, deduplicar dentro de la carga y detectar conflictos internos
        ResultadoCarga carga = cargador.cargarDesde(csv);
        List<ConflictoDeDuplicado> conflictos = new ArrayList<>(carga.conflictos());

        // 2. Persistencia: guardar solo los contratos nuevos (idempotencia — HU-07)
        int nuevos = 0;
        for (Contrato c : carga.contratos()) {
            BigDecimal monto = aMonto(c.monto());
            LocalDate fecha = aFecha(c.fecha());

            EntidadJpa entidad = entidadRepo.findByNombre(c.entidad())
                    .orElseGet(() -> entidadRepo.save(new EntidadJpa(c.entidad())));

            var existente = contratoRepo.findByNumeroContratoAndEntidad(c.numeroContrato(), entidad);
            if (existente.isPresent()) {
                // HU-06: el contrato ya estaba en la base; si trae datos distintos, se reporta
                List<String> distintos = camposDistintos(existente.get(), c, monto, fecha);
                if (!distintos.isEmpty()) {
                    conflictos.add(new ConflictoDeDuplicado(carga.filaDe(c), c.numeroContrato(),
                            c.entidad(), ConflictoDeDuplicado.YA_EN_LA_BASE, distintos));
                }
                continue;
            }

            ContratistaJpa contratista = contratistaRepo.findByNombre(c.contratista())
                    .orElseGet(() -> contratistaRepo.save(new ContratistaJpa(c.contratista())));

            FuncionarioJpa funcionario = funcionarioRepo.findByNombre(c.funcionario())
                    .orElseGet(() -> funcionarioRepo.save(new FuncionarioJpa(c.funcionario())));

            contratoRepo.save(new ContratoJpa(
                    c.numeroContrato(), entidad, contratista, funcionario, monto, fecha));
            nuevos++;
        }

        // 3. Detección sobre el histórico completo en BD (HU-07)
        List<Contrato> historico = historico();
        List<Alerta> alertas = detector.detectar(historico);
        List<AlertaFraccionamiento> fraccionamientos = detectorFraccionamiento.detectar(historico);

        // 4. Persistir cada alerta de reincidencia con su evidencia (HU-02)
        alertas.forEach(this::guardarAlerta);

        return new ResultadoServicio(nuevos, historico.size(), carga.duplicadosIgnorados(),
                carga.filasDescartadas(), conflictos, alertas, fraccionamientos);
    }

    /**
     * Estado actual: las dos señales sobre todo lo que hay en la base. Lo usa
     * la pantalla al abrirse y la exportación (HU-08).
     */
    @Transactional(readOnly = true)
    public ResumenDeAlertas consultarAlertas() {
        List<Contrato> historico = historico();
        return new ResumenDeAlertas(historico.size(),
                detector.detectar(historico),
                detectorFraccionamiento.detectar(historico));
    }

    /** HU-08: el reporte de alertas como CSV listo para Excel. */
    @Transactional(readOnly = true)
    public String exportarAlertas() {
        ResumenDeAlertas resumen = consultarAlertas();
        return ExportadorDeAlertas.exportar(resumen.alertas(), resumen.alertasFraccionamiento());
    }

    /**
     * Todos los contratos de la base, en el orden en que se cargaron. El orden
     * fijo importa: con nombres normalizados (HU-05) la alerta toma el nombre
     * del primer contrato del par, y debe ser siempre el mismo.
     */
    private List<Contrato> historico() {
        return contratoRepo.findAllConFetch().stream()
                .map(j -> Contrato.de(
                        j.getNumeroContrato(),
                        j.getEntidad().getNombre(),
                        j.getContratista().getNombre(),
                        j.getFuncionario().getNombre(),
                        j.getMonto() == null ? "" : j.getMonto().toPlainString(),
                        j.getFecha() == null ? "" : j.getFecha().toString()))
                .toList();
    }

    /** HU-06: compara la versión guardada con la que llega en el archivo. */
    private static List<String> camposDistintos(ContratoJpa guardado, Contrato nuevo,
                                                BigDecimal monto, LocalDate fecha) {
        List<String> distintos = new ArrayList<>();
        if (!guardado.getContratista().getNombre().equals(nuevo.contratista())) distintos.add("contratista");
        if (!guardado.getFuncionario().getNombre().equals(nuevo.funcionario())) distintos.add("funcionario");
        if (!mismoMonto(guardado.getMonto(), monto)) distintos.add("monto");
        if (!java.util.Objects.equals(guardado.getFecha(), fecha)) distintos.add("fecha");
        return distintos;
    }

    private static boolean mismoMonto(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return a == b;
        return a.compareTo(b) == 0;
    }

    /**
     * El dominio trata monto y fecha como texto (R-8) y R-6 dice que estar
     * vacíos no descarta la fila: la señal de reincidencia no los usa. Aquí
     * el vacío se convierte en NULL en lugar de romper la carga completa.
     * Un valor mal escrito (no vacío) sí rechaza el archivo con 400.
     */
    private static BigDecimal aMonto(String monto) {
        if (monto == null || monto.isBlank()) return null;
        try {
            return new BigDecimal(monto.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Monto no válido: \"" + monto + "\"");
        }
    }

    private static LocalDate aFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) return null;
        try {
            return LocalDate.parse(fecha.strip());
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha no válida (se espera AAAA-MM-DD): \"" + fecha + "\"");
        }
    }

    /**
     * Guarda la alerta y los contratos que la originaron. Es idempotente:
     * uq_alerta_par admite una sola alerta por par y tipo, así que volver a
     * correr la detección sobre los mismos datos no duplica nada — la misma
     * idea del problema duro, aplicada al resultado. Si el par ya tenía
     * alerta y aparecieron contratos nuevos, se suman a su evidencia.
     */
    private void guardarAlerta(Alerta alerta) {
        ContratistaJpa contratista = contratistaRepo.findByNombre(alerta.contratista()).orElseThrow();
        FuncionarioJpa funcionario = funcionarioRepo.findByNombre(alerta.funcionario()).orElseThrow();

        AlertaJpa fila = alertaRepo
                .findByContratistaAndFuncionarioAndTipo(contratista, funcionario, AlertaJpa.REINCIDENCIA)
                .orElseGet(() -> alertaRepo.save(new AlertaJpa(contratista, funcionario)));

        for (Contrato c : alerta.evidencia()) {
            entidadRepo.findByNombre(c.entidad())
                    .flatMap(e -> contratoRepo.findByNumeroContratoAndEntidad(c.numeroContrato(), e))
                    .ifPresent(fila.getEvidencia()::add);
        }
        alertaRepo.save(fila);
    }
}
