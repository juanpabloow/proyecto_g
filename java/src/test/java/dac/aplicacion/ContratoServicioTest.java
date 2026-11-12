package dac.aplicacion;

import dac.dominio.CargadorDeContratos;
import dac.dominio.DetectorDeFraccionamiento;
import dac.dominio.DetectorDeReincidencias;
import dac.persistencia.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de la capa de aplicación con Mockito: los repositorios
 * son simulados, el dominio es real. Aíslan la regla de {@code ContratoServicio}
 * —qué se guarda y qué no— sin levantar Spring ni PostgreSQL.
 *
 * <p>Las pruebas de punta a punta ({@code ContratoE2ETest} y demás) no usan
 * simulaciones a propósito: entran por HTTP real contra la base real
 * (Decisión 16). Ver decisiones-tecnicas.md.
 */
@ExtendWith(MockitoExtension.class)
class ContratoServicioTest {

    private static final String ENCABEZADO = "numero_contrato,entidad,contratista,funcionario,monto,fecha\n";

    @Mock private EntidadRepo entidadRepo;
    @Mock private ContratistaRepo contratistaRepo;
    @Mock private FuncionarioRepo funcionarioRepo;
    @Mock private ContratoRepo contratoRepo;
    @Mock private AlertaRepo alertaRepo;

    private ContratoServicio servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ContratoServicio(new CargadorDeContratos(), new DetectorDeReincidencias(),
                new DetectorDeFraccionamiento(), entidadRepo, contratistaRepo, funcionarioRepo,
                contratoRepo, alertaRepo);
    }

    @Test
    @DisplayName("HU-07 / R-2: un contrato cuya clave natural ya está guardada no se vuelve a guardar")
    void no_guarda_un_contrato_cuya_clave_ya_existe() {
        // Preparar: la base ya tiene el contrato 001 de la Alcaldía
        EntidadJpa alcaldia = new EntidadJpa("Alcaldía");
        ContratistaJpa acme = new ContratistaJpa("ACME SAS");
        FuncionarioJpa juan = new FuncionarioJpa("Juan Pérez");
        ContratoJpa guardado = new ContratoJpa("001", alcaldia, acme, juan,
                new BigDecimal("100"), LocalDate.parse("2026-01-01"));
        when(entidadRepo.findAll()).thenReturn(List.of(alcaldia));
        when(contratistaRepo.findAll()).thenReturn(List.of(acme));
        when(funcionarioRepo.findAll()).thenReturn(List.of(juan));
        when(contratoRepo.findAllConFetch()).thenReturn(List.of(guardado));

        // Ejecutar: llega otra vez el mismo contrato
        ResultadoServicio r = servicio.cargarYDetectar(csv("001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"));

        // Verificar: no se escribe nada y la recarga queda contada
        verify(contratoRepo, never()).save(any());
        verify(entidadRepo, never()).save(any());
        verify(contratistaRepo, never()).save(any());
        verify(funcionarioRepo, never()).save(any());
        assertEquals(0, r.contratosNuevos(), "no hay contratos nuevos");
        assertEquals(1, r.contratosYaGuardados(), "el contrato se reconoce como ya guardado");
        assertTrue(r.conflictos().isEmpty(), "trae los mismos datos: no es un conflicto");
    }

    @Test
    @DisplayName("HU-01 / problema duro: un contrato repetido en el archivo se guarda una sola vez, con monto y fecha convertidos")
    void guarda_una_sola_vez_el_contrato_repetido_en_el_archivo() {
        // Preparar: base vacía; cada save devuelve la misma entidad que recibe
        when(entidadRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(contratistaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(funcionarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(contratoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Ejecutar: el 002 viene dos veces en el mismo archivo
        ResultadoServicio r = servicio.cargarYDetectar(csv(
                "002,Gobernación,Otra Ltda,Ana Ruiz,250.50,2026-02-10\n"
                + "002,Gobernación,Otra Ltda,Ana Ruiz,250.50,2026-02-10\n"));

        // Verificar: un solo INSERT, con los tipos de la base
        ArgumentCaptor<ContratoJpa> guardado = ArgumentCaptor.forClass(ContratoJpa.class);
        verify(contratoRepo, times(1)).save(guardado.capture());
        assertEquals("002", guardado.getValue().getNumeroContrato());
        assertEquals(0, new BigDecimal("250.50").compareTo(guardado.getValue().getMonto()));
        assertEquals(LocalDate.of(2026, 2, 10), guardado.getValue().getFecha());
        assertEquals(1, r.contratosNuevos());
        assertEquals(1, r.duplicadosIgnorados(), "la repetición se cuenta, no se guarda");
    }

    @Test
    @DisplayName("R-8: un monto negativo rechaza el archivo antes de tocar la base")
    void un_monto_negativo_no_toca_la_base() {
        // Preparar: la fila 3 trae un monto negativo
        InputStream archivo = csv("003,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
                + "004,Alcaldía,ACME SAS,Juan Pérez,-5,2026-01-02\n");

        // Ejecutar
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.cargarYDetectar(archivo));

        // Verificar: el mensaje nombra la fila y ningún repositorio se usó
        assertTrue(error.getMessage().contains("Fila 3"), error.getMessage());
        verifyNoInteractions(entidadRepo, contratistaRepo, funcionarioRepo, contratoRepo, alertaRepo);
    }

    private static InputStream csv(String filas) {
        return new ByteArrayInputStream((ENCABEZADO + filas).getBytes(StandardCharsets.UTF_8));
    }
}
