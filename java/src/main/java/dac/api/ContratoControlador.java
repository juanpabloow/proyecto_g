package dac.api;

import dac.aplicacion.ContratoServicio;
import dac.aplicacion.ResultadoServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Endpoint REST de la rebanada del esqueleto andante.
 *
 * POST /api/contratos/cargar
 *   Content-Type: multipart/form-data
 *   Param: archivo (CSV)
 *
 * Cruza la frontera HTTP/UI que antes faltaba en el walking skeleton.
 */
@RestController
@RequestMapping("/api/contratos")
public class ContratoControlador {

    private final ContratoServicio servicio;

    public ContratoControlador(ContratoServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/cargar")
    public ResponseEntity<ResultadoServicio> cargar(@RequestParam("archivo") MultipartFile archivo) {
        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            ResultadoServicio resultado = servicio.cargarYDetectar(archivo.getInputStream());
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
