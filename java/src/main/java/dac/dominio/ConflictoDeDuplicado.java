package dac.dominio;

import java.util.List;

/**
 * HU-06 — Un contrato repetido que trae datos distintos a la versión que ya
 * se conservó. El contrato se carga una sola vez (gana la primera versión,
 * Decisión 6), pero la diferencia se reporta en vez de perderse en silencio.
 *
 * @param fila             fila del archivo que traía la versión distinta
 * @param numeroContrato   número del contrato en conflicto
 * @param entidad          entidad del contrato en conflicto
 * @param conflictoCon     con qué versión choca: "fila N" del mismo archivo
 *                         o el contrato ya guardado en la base
 * @param camposDistintos  qué campos difieren
 */
public record ConflictoDeDuplicado(
        int fila,
        String numeroContrato,
        String entidad,
        String conflictoCon,
        List<String> camposDistintos) {

    public static final String YA_EN_LA_BASE = "contrato ya guardado en la base";

    public ConflictoDeDuplicado {
        camposDistintos = List.copyOf(camposDistintos);
    }
}
