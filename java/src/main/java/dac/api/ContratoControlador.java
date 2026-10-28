package dac.api;

import dac.aplicacion.ContratoServicio;
import dac.aplicacion.ResultadoServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * POST /api/contratos/cargar
 *   Content-Type: multipart/form-data
 *   Param: archivo (CSV)
 *
 * Responde 200 con el resultado de la carga, o 400 con {"error": "..."} si el
 * archivo no se puede cargar (R-1). El mensaje lo muestra la interfaz web.
 */
@RestController
@RequestMapping("/api/contratos")
public class ContratoControlador {

    private final ContratoServicio servicio;

    public ContratoControlador(ContratoServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/cargar")
    public ResponseEntity<?> cargar(@RequestParam("archivo") MultipartFile archivo) {
        if (archivo.isEmpty()) {
            return error("El archivo está vacío");
        }
        try {
            ResultadoServicio resultado = servicio.cargarYDetectar(archivo.getInputStream());
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo leer el archivo"));
        }
    }

    private static ResponseEntity<Map<String, String>> error(String mensaje) {
        return ResponseEntity.badRequest().body(Map.of("error", mensaje == null ? "Archivo no válido" : mensaje));
    }
}
