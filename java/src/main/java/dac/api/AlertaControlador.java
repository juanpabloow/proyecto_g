package dac.api;

import dac.aplicacion.ContratoServicio;
import dac.aplicacion.ResumenDeAlertas;
import dac.seguridad.Auditoria;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * GET /api/alertas            → estado actual de las dos señales (JSON)
 * GET /api/alertas/exportar   → el mismo reporte como CSV para Excel (HU-08)
 */
@RestController
@RequestMapping("/api/alertas")
public class AlertaControlador {

    static final String NOMBRE_ARCHIVO = "alertas_dac.csv";

    private final ContratoServicio servicio;

    public AlertaControlador(ContratoServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public ResumenDeAlertas consultar() {
        return servicio.consultarAlertas();
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar() {
        String texto = servicio.exportarAlertas();
        Auditoria.exportacion(Math.max(0, texto.split("\r\n").length - 1));
        byte[] csv = texto.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + NOMBRE_ARCHIVO + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }
}
