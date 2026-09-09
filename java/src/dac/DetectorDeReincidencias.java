package dac;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La señal del MVP: el mismo contratista con el mismo funcionario en más de
 * un contrato (R-3).
 *
 * <p>Agrupa por el par y genera <b>una</b> alerta por par —no una por
 * contrato— con todos sus contratos como evidencia. Un mismo contratista con
 * dos funcionarios distintos no es señal: la unidad es el par.
 *
 * <p>Lo que esta clase deliberadamente no hace: no mira el monto, no mira la
 * fecha y no cruza nombres parecidos (R-3, R-5).
 */
public final class DetectorDeReincidencias {

    /**
     * A partir de cuántos contratos el par se considera reincidente.
     *
     * <p>Es un supuesto <b>sin validar</b>: nadie de S-1 ha confirmado que el
     * umbral correcto sea 2 ni que deba ignorarse la ventana de tiempo (R-3).
     * Está como campo —y no incrustado en el {@code if}— para que el día que
     * se valide, el cambio sea un parámetro y no una reescritura.
     */
    private final int umbral = 2;

    /** Devuelve una alerta por cada par reincidente, con su evidencia. */
    public List<Alerta> detectar(List<Contrato> contratos) {
        Map<String, List<Contrato>> grupos = new LinkedHashMap<>();
        for (Contrato contrato : contratos) {
            grupos.computeIfAbsent(contrato.par(), par -> new ArrayList<>()).add(contrato);
        }

        List<Alerta> alertas = new ArrayList<>();
        for (List<Contrato> grupo : grupos.values()) {
            if (grupo.size() >= umbral) {
                Contrato cualquiera = grupo.get(0);
                alertas.add(new Alerta(cualquiera.contratista(), cualquiera.funcionario(), grupo));
            }
        }
        return List.copyOf(alertas);
    }
}
