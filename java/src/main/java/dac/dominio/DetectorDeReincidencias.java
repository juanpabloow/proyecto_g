package dac.dominio;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * La señal del MVP: el mismo contratista con el mismo funcionario
 * en más de un contrato (R-3).
 */
@Component
public final class DetectorDeReincidencias {

    private final int umbral = 2;

    public List<Alerta> detectar(List<Contrato> contratos) {
        Map<String, List<Contrato>> grupos = new LinkedHashMap<>();
        for (Contrato contrato : contratos) {
            grupos.computeIfAbsent(contrato.par(), p -> new ArrayList<>()).add(contrato);
        }

        List<Alerta> alertas = new ArrayList<>();
        for (List<Contrato> grupo : grupos.values()) {
            if (grupo.size() >= umbral) {
                Contrato c = grupo.get(0);
                alertas.add(new Alerta(c.contratista(), c.funcionario(), grupo));
            }
        }
        return List.copyOf(alertas);
    }
}
