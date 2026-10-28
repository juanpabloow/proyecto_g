package dac.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * HU-03 — Segunda señal: fraccionamiento de contratos (R-9).
 *
 * <p>Regla provisional (Decisión 20), pendiente de validar con S-1:
 * se alerta cuando el <b>mismo contratista</b>, en la <b>misma entidad</b>,
 * tiene <b>2 o más contratos</b>, cada uno <b>por debajo del tope</b>, cuyas
 * fechas caben en una <b>ventana de N días</b> y cuya <b>suma alcanza o
 * supera el tope</b>. Es el patrón de partir un contrato grande en varios
 * pequeños para no pasar por un proceso de mayor cuantía.
 *
 * <p>El tope y la ventana son parámetros ({@code application.properties}),
 * porque el tope real depende del presupuesto de cada entidad.
 * Los contratos sin monto o sin fecha no participan: la regla no se puede
 * evaluar sobre ellos (R-8).
 */
public final class DetectorDeFraccionamiento {

    public static final BigDecimal TOPE_POR_DEFECTO = new BigDecimal("100000000");
    public static final int VENTANA_POR_DEFECTO = 30;

    private final BigDecimal tope;
    private final int ventanaDias;

    public DetectorDeFraccionamiento() {
        this(TOPE_POR_DEFECTO, VENTANA_POR_DEFECTO);
    }

    public DetectorDeFraccionamiento(BigDecimal tope, int ventanaDias) {
        if (tope == null || tope.signum() <= 0) {
            throw new IllegalArgumentException("El tope de fraccionamiento debe ser mayor que cero");
        }
        if (ventanaDias < 1) {
            throw new IllegalArgumentException("La ventana de fraccionamiento debe ser de al menos 1 día");
        }
        this.tope = tope;
        this.ventanaDias = ventanaDias;
    }

    public List<AlertaFraccionamiento> detectar(List<Contrato> contratos) {
        Map<String, List<ContratoConValores>> grupos = new LinkedHashMap<>();
        for (Contrato c : contratos) {
            ContratoConValores conValores = ContratoConValores.de(c);
            if (conValores == null || conValores.monto().compareTo(tope) >= 0) continue;
            grupos.computeIfAbsent(c.contratistaEnEntidad(), k -> new ArrayList<>()).add(conValores);
        }

        List<AlertaFraccionamiento> alertas = new ArrayList<>();
        for (List<ContratoConValores> grupo : grupos.values()) {
            grupo.sort(Comparator.comparing(ContratoConValores::fecha)
                    .thenComparing(x -> x.contrato().numeroContrato()));
            alertas.addAll(buscarEnGrupo(grupo));
        }
        return List.copyOf(alertas);
    }

    /**
     * Recorre el grupo ordenado por fecha. Desde cada contrato toma todos los
     * que caben en la ventana; si son 2 o más y suman el tope, es una alerta
     * y la búsqueda sigue después del último contrato usado, para no reportar
     * dos veces la misma evidencia.
     */
    private List<AlertaFraccionamiento> buscarEnGrupo(List<ContratoConValores> grupo) {
        List<AlertaFraccionamiento> alertas = new ArrayList<>();
        int inicio = 0;
        while (inicio < grupo.size()) {
            LocalDate limite = grupo.get(inicio).fecha().plusDays(ventanaDias);
            int fin = inicio;
            BigDecimal suma = BigDecimal.ZERO;
            while (fin < grupo.size() && !grupo.get(fin).fecha().isAfter(limite)) {
                suma = suma.add(grupo.get(fin).monto());
                fin++;
            }
            if (fin - inicio >= 2 && suma.compareTo(tope) >= 0) {
                List<Contrato> evidencia = grupo.subList(inicio, fin).stream()
                        .map(ContratoConValores::contrato).toList();
                Contrato primero = evidencia.get(0);
                alertas.add(new AlertaFraccionamiento(primero.contratista(), primero.entidad(),
                        evidencia, suma, tope, ventanaDias));
                inicio = fin;
            } else {
                inicio++;
            }
        }
        return alertas;
    }

    public BigDecimal tope() { return tope; }

    public int ventanaDias() { return ventanaDias; }

    private record ContratoConValores(Contrato contrato, BigDecimal monto, LocalDate fecha) {

        static ContratoConValores de(Contrato c) {
            if (c.monto() == null || c.monto().isBlank() || c.fecha() == null || c.fecha().isBlank()) {
                return null;
            }
            try {
                return new ContratoConValores(c, new BigDecimal(c.monto().strip()),
                        LocalDate.parse(c.fecha().strip()));
            } catch (NumberFormatException | DateTimeParseException e) {
                return null;
            }
        }
    }
}
