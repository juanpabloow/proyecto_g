package dac.aplicacion;

import dac.dominio.DetectorDeFraccionamiento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Parámetros de las señales que dependen del negocio (Decisión 20). Viven en
 * application.properties para que cambiarlos no exija tocar el dominio.
 */
@Configuration
public class ConfiguracionDeSenales {

    @Bean
    public DetectorDeFraccionamiento detectorDeFraccionamiento(
            @Value("${dac.fraccionamiento.tope:100000000}") BigDecimal tope,
            @Value("${dac.fraccionamiento.ventana-dias:30}") int ventanaDias) {
        return new DetectorDeFraccionamiento(tope, ventanaDias);
    }
}
