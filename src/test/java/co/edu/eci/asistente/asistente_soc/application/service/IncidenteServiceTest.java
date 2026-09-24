package co.edu.eci.asistente.asistente_soc.application.service;

import co.edu.eci.asistente.asistente_soc.domain.exception.IncidenteNoEncontradoException;
import co.edu.eci.asistente.asistente_soc.domain.model.EstadoIncidente;
import co.edu.eci.asistente.asistente_soc.domain.model.Incidente;
import co.edu.eci.asistente.asistente_soc.domain.ports.out.AsistenteIaPort;
import co.edu.eci.asistente.asistente_soc.domain.ports.out.IncidenteRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidenteServiceTest {

    @Mock
    private IncidenteRepositoryPort repositoryPort;

    @Mock
    private AsistenteIaPort iaPort;

    @InjectMocks
    private IncidenteService service;

    @Test
    void procesarNuevoIncidente_deberiaGuardarTriarYGuardar() {
        Incidente incidente = Incidente.builder().descripcion("Intento de phishing").build();
        when(repositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        doAnswer(inv -> {
            Incidente i = inv.getArgument(0);
            i.marcarParaAprobacion("PHISHING", "ALTA", "accion", "cmd", "srv", "1.2.3.4");
            return null;
        }).when(iaPort).analizarYProponerContencion(any());

        Incidente resultado = service.procesarNuevoIncidente(incidente);

        verify(repositoryPort, times(2)).guardar(any());
        verify(iaPort).analizarYProponerContencion(any());
        assertEquals(EstadoIncidente.ESPERANDO_APROBACION_HUMANA, resultado.getEstado());
        assertEquals("PHISHING", resultado.getTipo());
        assertEquals("cmd", resultado.getComandoEjecucion());
        assertEquals("1.2.3.4", resultado.getIpBloqueada());
    }

    @Test
    void procesarNuevoIncidente_conFalloIa_deberiaDegradarAManual() {
        Incidente incidente = Incidente.builder().descripcion("Alerta").build();
        when(repositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        doAnswer(inv -> {
            Incidente i = inv.getArgument(0);
            i.marcarParaAprobacion("OTRO", "MEDIA", "No se pudo contactar al modelo de lenguaje");
            return null;
        }).when(iaPort).analizarYProponerContencion(any());

        Incidente resultado = service.procesarNuevoIncidente(incidente);

        assertEquals("OTRO", resultado.getTipo());
        assertEquals(EstadoIncidente.ESPERANDO_APROBACION_HUMANA, resultado.getEstado());
    }

    @Test
    void registrarDecisionHumana_aprobado_deberiaCambiarEstado() {
        Incidente incidente = Incidente.builder()
                .id("abc")
                .estado(EstadoIncidente.ESPERANDO_APROBACION_HUMANA)
                .build();
        when(repositoryPort.buscarPorId("abc")).thenReturn(Optional.of(incidente));
        when(repositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Incidente resultado = service.registrarDecisionHumana("abc", true);

        assertEquals(EstadoIncidente.APROBADO, resultado.getEstado());
    }

    @Test
    void registrarDecisionHumana_rechazado_deberiaCambiarEstado() {
        Incidente incidente = Incidente.builder()
                .id("abc")
                .estado(EstadoIncidente.ESPERANDO_APROBACION_HUMANA)
                .build();
        when(repositoryPort.buscarPorId("abc")).thenReturn(Optional.of(incidente));
        when(repositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Incidente resultado = service.registrarDecisionHumana("abc", false);

        assertEquals(EstadoIncidente.RECHAZADO, resultado.getEstado());
    }

    @Test
    void registrarDecisionHumana_incidenteInexistente_deberiaLanzarExcepcion() {
        when(repositoryPort.buscarPorId("no-existe")).thenReturn(Optional.empty());

        assertThrows(IncidenteNoEncontradoException.class,
                () -> service.registrarDecisionHumana("no-existe", true));
    }
}