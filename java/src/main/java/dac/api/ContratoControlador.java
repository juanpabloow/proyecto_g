package dac.api;

import dac.aplicacion.ContratoServicio;
import dac.aplicacion.ResultadoServicio;
import dac.seguridad.Auditoria;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * POST /api/contratos/cargar
 *   Content-Type: multipart/form-data
 *   Param: archivo (CSV, máximo 10 MB)
 *
 * Responde 200 con el resultado de la carga, o 400 con {"error": "..."} si el
 * archivo no se puede cargar (R-1). Cada carga, aceptada o rechazada, queda
 * en el registro de auditoría (Módulo 7).
 */
@RestController
@RequestMapping("/api/contratos")
public class ContratoControlador {

    static final String SOLO_CSV = "Solo se aceptan archivos .csv";

    private final ContratoServicio servicio;

    public ContratoControlador(ContratoServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/cargar")
    public ResponseEntity<?> cargar(@RequestParam("archivo") MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (archivo.isEmpty()) {
            return rechazar(nombre, "El archivo está vacío");
        }
        if (nombre == null || !nombre.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            return rechazar(nombre, SOLO_CSV);
        }
        try {
            ResultadoServicio r = servicio.cargarYDetectar(archivo.getInputStream());
            Auditoria.cargaAceptada(nombre, archivo.getSize(), r.contratosNuevos(), r.totalEnBd(),
                    r.filasDescartadas().size(), r.conflictos().size(), r.alertas().size(),
                    r.alertasFraccionamiento().size());
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            return rechazar(nombre, e.getMessage());
        } catch (IOException e) {
            Auditoria.errorInesperado("lectura del archivo", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo leer el archivo"));
        }
    }

    private static ResponseEntity<Map<String, String>> rechazar(String archivo, String mensaje) {
        String texto = mensaje == null ? "Archivo no válido" : mensaje;
        Auditoria.cargaRechazada(archivo, texto);
        return ResponseEntity.badRequest().body(Map.of("error", texto));
    }
}
