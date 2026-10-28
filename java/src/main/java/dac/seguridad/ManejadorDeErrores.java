package dac.seguridad;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

/**
 * Errores con un mensaje útil para el analista y sin detalles internos
 * (Módulo 7, S-05). Lo demás lo responde Spring sin traza ni mensaje
 * ({@code server.error.include-*=never} en application.properties).
 */
@RestControllerAdvice
public class ManejadorDeErrores {

    static final String MENSAJE_TAMANO = "El archivo supera el máximo de 10 MB";
    static final String MENSAJE_CONFLICTO =
            "La carga chocó con datos que se guardaron al mismo tiempo y no se guardó nada. "
            + "Vuelve a cargar el archivo: los contratos ya guardados no se duplican.";

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> archivoDemasiadoGrande(MaxUploadSizeExceededException e) {
        Auditoria.cargaRechazada("(sin leer)", MENSAJE_TAMANO);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("error", MENSAJE_TAMANO));
    }

    /**
     * DT-09: dos cargas simultáneas del mismo contrato. La base lo impide
     * (uq_contrato_clave_natural) y la transacción se deshace completa; antes
     * el analista veía un 500 sin explicación.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflictoDeIntegridad(DataIntegrityViolationException e) {
        Auditoria.errorInesperado("carga (violación de integridad)", e);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", MENSAJE_CONFLICTO));
    }
}
