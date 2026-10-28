package dac.aplicacion;

import dac.dominio.*;
import dac.persistencia.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Orquesta la rebanada completa:
 * CSV → dominio → persistencia → detección → respuesta.
 *
 * <p>Rendimiento (RNF-1, Módulo 7): antes se hacían varias consultas por
 * cada fila del archivo (buscar entidad, contratista, funcionario y
 * contrato), y cada consulta obligaba a Hibernate a revisar todo lo cargado
 * hasta ese momento. Ahora se lee lo que ya existe <b>una sola vez</b> al
 * empezar y se trabaja con índices en memoria; a la base solo van las
 * inserciones. La idempotencia no cambia: la sigue garantizando
 * {@code uq_contrato_clave_natural}.
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

        // 2. Validar tipos ANTES de escribir: un archivo con un valor malo no deja nada a medias
        Map<Contrato, BigDecimal> montos = new HashMap<>();
        Map<Contrato, LocalDate> fechas = new HashMap<>();
        for (Contrato c : carga.contratos()) {
            montos.put(c, aMonto(c.monto(), carga.filaDe(c)));
            fechas.put(c, aFecha(c.fecha(), carga.filaDe(c)));
        }

        // 3. Lo que ya existe, leído una sola vez
        Indice indice = new Indice();

        // 4. Persistencia: guardar solo los contratos nuevos (idempotencia — HU-07)
        int nuevos = 0;
        for (Contrato c : carga.contratos()) {
            ContratoJpa existente = indice.contratos.get(c.claveNatural());
            if (existente != null) {
                // HU-06: el contrato ya estaba en la base; si trae datos distintos, se reporta
                List<String> distintos = camposDistintos(existente, c, montos.get(c), fechas.get(c));
                if (!distintos.isEmpty()) {
                    conflictos.add(new ConflictoDeDuplicado(carga.filaDe(c), c.numeroContrato(),
                            c.entidad(), ConflictoDeDuplicado.YA_EN_LA_BASE, distintos));
                }
                continue;
            }
            ContratoJpa nuevo = contratoRepo.save(new ContratoJpa(c.numeroContrato(),
                    indice.entidad(c.entidad()), indice.contratista(c.contratista()),
                    indice.funcionario(c.funcionario()), montos.get(c), fechas.get(c)));
            indice.agregar(nuevo);
            nuevos++;
        }

        // 5. Detección sobre el histórico completo (HU-07), en orden de carga
        List<Contrato> historico = indice.historico();
        List<Alerta> alertas = detector.detectar(historico);
        List<AlertaFraccionamiento> fraccionamientos = detectorFraccionamiento.detectar(historico);

        // 6. Persistir cada alerta de reincidencia con su evidencia (HU-02)
        Map<String, AlertaJpa> alertasGuardadas = new HashMap<>();
        alertaRepo.findAll().forEach(a -> alertasGuardadas.put(clave(a.getContratista(), a.getFuncionario()), a));
        for (Alerta alerta : alertas) {
            guardarAlerta(alerta, indice, alertasGuardadas);
        }

        return new ResultadoServicio(nuevos, historico.size(), carga.duplicadosIgnorados(),
                carga.filasDescartadas(), conflictos, alertas, fraccionamientos);
    }

    /**
     * Estado actual: las dos señales sobre todo lo que hay en la base. Lo usa
     * la pantalla al abrirse y la exportación (HU-08).
     */
    @Transactional(readOnly = true)
    public ResumenDeAlertas consultarAlertas() {
        List<Contrato> historico = contratoRepo.findAllConFetch().stream().map(ContratoServicio::aDominio).toList();
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
     * Guarda la alerta y los contratos que la originaron. Es idempotente:
     * uq_alerta_par admite una sola alerta por par y tipo, así que volver a
     * correr la detección sobre los mismos datos no duplica nada. Si el par ya
     * tenía alerta y aparecieron contratos nuevos, se suman a su evidencia.
     */
    private void guardarAlerta(Alerta alerta, Indice indice, Map<String, AlertaJpa> guardadas) {
        ContratoJpa primero = indice.contratos.get(alerta.evidencia().get(0).claveNatural());
        ContratistaJpa contratista = primero.getContratista();
        FuncionarioJpa funcionario = primero.getFuncionario();

        AlertaJpa fila = guardadas.computeIfAbsent(clave(contratista, funcionario),
                k -> alertaRepo.save(new AlertaJpa(contratista, funcionario)));

        for (Contrato c : alerta.evidencia()) {
            ContratoJpa contrato = indice.contratos.get(c.claveNatural());
            if (contrato != null) fila.getEvidencia().add(contrato);
        }
        alertaRepo.save(fila);
    }

    private static String clave(ContratistaJpa contratista, FuncionarioJpa funcionario) {
        return contratista.getId() + ":" + funcionario.getId();
    }

    private static Contrato aDominio(ContratoJpa j) {
        return Contrato.de(
                j.getNumeroContrato(),
                j.getEntidad().getNombre(),
                j.getContratista().getNombre(),
                j.getFuncionario().getNombre(),
                j.getMonto() == null ? "" : j.getMonto().toPlainString(),
                j.getFecha() == null ? "" : j.getFecha().toString());
    }

    /** HU-06: compara la versión guardada con la que llega en el archivo. */
    private static List<String> camposDistintos(ContratoJpa guardado, Contrato nuevo,
                                                BigDecimal monto, LocalDate fecha) {
        List<String> distintos = new ArrayList<>();
        if (!guardado.getContratista().getNombre().equals(nuevo.contratista())) distintos.add("contratista");
        if (!guardado.getFuncionario().getNombre().equals(nuevo.funcionario())) distintos.add("funcionario");
        if (!mismoMonto(guardado.getMonto(), monto)) distintos.add("monto");
        if (!Objects.equals(guardado.getFecha(), fecha)) distintos.add("fecha");
        return distintos;
    }

    private static boolean mismoMonto(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return a == b;
        return a.compareTo(b) == 0;
    }

    /**
     * El dominio trata monto y fecha como texto (R-8) y R-6 dice que estar
     * vacíos no descarta la fila. Un valor mal escrito o un monto negativo sí
     * rechazan el archivo con 400 y un mensaje que nombra la fila (Módulo 7:
     * validación en la frontera, antes de que la base responda con un 500).
     */
    private static BigDecimal aMonto(String monto, int fila) {
        if (monto == null || monto.isBlank()) return null;
        BigDecimal valor;
        try {
            valor = new BigDecimal(monto.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Fila " + fila + ": monto no válido \"" + recorte(monto) + "\"");
        }
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("Fila " + fila + ": el monto no puede ser negativo (R-8)");
        }
        return valor;
    }

    private static LocalDate aFecha(String fecha, int fila) {
        if (fecha == null || fecha.isBlank()) return null;
        try {
            return LocalDate.parse(fecha.strip());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fila " + fila + ": fecha no válida \"" + recorte(fecha)
                    + "\" (se espera AAAA-MM-DD)");
        }
    }

    private static String recorte(String valor) {
        return valor.length() > 40 ? valor.substring(0, 40) + "…" : valor;
    }

    /**
     * Lo que ya está en la base, por nombre o por clave natural, para no
     * consultar fila por fila. Vive solo durante una carga.
     */
    private final class Indice {
        final Map<String, EntidadJpa> entidades = new HashMap<>();
        final Map<String, ContratistaJpa> contratistas = new HashMap<>();
        final Map<String, FuncionarioJpa> funcionarios = new HashMap<>();
        final Map<String, ContratoJpa> contratos = new LinkedHashMap<>();

        Indice() {
            entidadRepo.findAll().forEach(e -> entidades.put(e.getNombre(), e));
            contratistaRepo.findAll().forEach(c -> contratistas.put(c.getNombre(), c));
            funcionarioRepo.findAll().forEach(f -> funcionarios.put(f.getNombre(), f));
            contratoRepo.findAllConFetch().forEach(this::agregar);
        }

        void agregar(ContratoJpa c) {
            contratos.put(Contrato.de(c.getNumeroContrato(), c.getEntidad().getNombre(), "", "", "", "")
                    .claveNatural(), c);
        }

        EntidadJpa entidad(String nombre) {
            return entidades.computeIfAbsent(nombre, n -> entidadRepo.save(new EntidadJpa(n)));
        }

        ContratistaJpa contratista(String nombre) {
            return contratistas.computeIfAbsent(nombre, n -> contratistaRepo.save(new ContratistaJpa(n)));
        }

        FuncionarioJpa funcionario(String nombre) {
            return funcionarios.computeIfAbsent(nombre, n -> funcionarioRepo.save(new FuncionarioJpa(n)));
        }

        /** En orden de carga: con HU-05 la alerta toma el nombre del primer contrato del par. */
        List<Contrato> historico() {
            return contratos.values().stream().map(ContratoServicio::aDominio).toList();
        }
    }
}
