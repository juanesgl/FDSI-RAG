package co.edu.eci.asistente.asistente_soc.infrastructure.in.rest.webhook;

import co.edu.eci.asistente.asistente_soc.domain.model.Incidente;
import co.edu.eci.asistente.asistente_soc.domain.ports.in.ClasificarIncidenteUseCase;
import co.edu.eci.asistente.asistente_soc.infrastructure.in.web.dto.AlertaSeguridadRequest;
import co.edu.eci.asistente.asistente_soc.infrastructure.in.web.mapper.AlertaWebMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookAlertaControllerTest {

    @Mock
    private ClasificarIncidenteUseCase clasificarIncidenteUseCase;

    @Mock
    private AlertaWebMapper alertaWebMapper;

    @InjectMocks
    private WebhookAlertaController controller;

    private AlertaSeguridadRequest alertaValida;

    @BeforeEach
    void setUp() {
        alertaValida = new AlertaSeguridadRequest(
                "esc-01-alert-001",
                OffsetDateTime.now(),
                new AlertaSeguridadRequest.Rule("5710", "Phishing email detected", 10, null, null, 1),
                null,
                Map.of(),
                "Email with suspicious link",
                "mail"
        );
    }

    @Test
    void recibirAlertaSiem_deberiaResponder202YDispararTriajeAsync() {
        when(alertaWebMapper.aDominio(any())).thenReturn(Incidente.builder().build());

        ResponseEntity<Map<String, String>> response = controller.recibirAlertaSiem(null, alertaValida);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody().get("id"));
        verify(clasificarIncidenteUseCase).procesarNuevoIncidenteAsync(any());
    }

    @Test
    void recibirAlertaSiem_conSecretIncorrecto_deberiaResponder401() {
        ReflectionTestUtils.setField(controller, "webhookSecret", "secreto-compartido");

        ResponseEntity<Map<String, String>> response = controller.recibirAlertaSiem("secreto-malo", alertaValida);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(clasificarIncidenteUseCase, never()).procesarNuevoIncidenteAsync(any());
    }

    @Test
    void recibirAlertaSiem_conSecretCorrecto_deberiaResponder202() {
        ReflectionTestUtils.setField(controller, "webhookSecret", "secreto-compartido");
        when(alertaWebMapper.aDominio(any())).thenReturn(Incidente.builder().build());

        ResponseEntity<Map<String, String>> response = controller.recibirAlertaSiem("secreto-compartido", alertaValida);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(clasificarIncidenteUseCase).procesarNuevoIncidenteAsync(any());
    }

    @Test
    void recibirAlertaSiem_sinSecretConfigurado_deberiaAceptar() {
        when(alertaWebMapper.aDominio(any())).thenReturn(Incidente.builder().build());

        ResponseEntity<Map<String, String>> response = controller.recibirAlertaSiem(null, alertaValida);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }
}