package co.edu.eci.asistente.asistente_soc.infrastructure.in.rest.webhook;

import co.edu.eci.asistente.asistente_soc.domain.model.Incidente;
import co.edu.eci.asistente.asistente_soc.domain.ports.in.ClasificarIncidenteUseCase;
import co.edu.eci.asistente.asistente_soc.infrastructure.in.web.dto.AlertaSeguridadRequest;
import co.edu.eci.asistente.asistente_soc.infrastructure.in.web.mapper.AlertaWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Adaptador de entrada Machine-to-Machine (Mision 2): recibe alertas directamente desde un
 * SIEM/SOC (p. ej. Wazuh, Splunk) sin intervencion humana. El triaje con IA ocurre de forma
 * asincrona: el SIEM recibe 202 Accepted con el id del incidente y no espera el resultado.
 */
@RestController
@RequestMapping("/api/v1/webhook/alertas")
@Tag(name = "Webhook SIEM", description = "Ingesta automatizada (Machine-to-Machine) de alertas "
        + "provenientes de un SIEM/SOC. Procesamiento asincrono: responde 202 Accepted con el id "
        + "del incidente y el triaje con IA ocurre en segundo plano.")
public class WebhookAlertaController {

    private final ClasificarIncidenteUseCase clasificarIncidenteUseCase;
    private final AlertaWebMapper alertaWebMapper;

    @Value("${webhook.secret:}")
    private String webhookSecret;

    public WebhookAlertaController(ClasificarIncidenteUseCase clasificarIncidenteUseCase,
                                   AlertaWebMapper alertaWebMapper) {
        this.clasificarIncidenteUseCase = clasificarIncidenteUseCase;
        this.alertaWebMapper = alertaWebMapper;
    }

    @Operation(summary = "Recibir alerta desde un SIEM (asincrono)",
            description = "Recibe el payload JSON de un SIEM, lo mapea a Incidente y dispara el "
                    + "triaje con IA de forma asincrona. El SIEM no espera el resultado: recibe "
                    + "202 Accepted con el id del incidente.")
    @ApiResponse(responseCode = "202", description = "Alerta aceptada, en procesamiento",
            content = @Content(schema = @Schema(example = "{\"id\": \"...\", \"estado\": \"EN_PROCESAMIENTO\"}")))
    @ApiResponse(responseCode = "400", description = "El payload no cumple con los campos obligatorios")
    @ApiResponse(responseCode = "401", description = "Secret del webhook incorrecto")
    @PostMapping
    public ResponseEntity<Map<String, String>> recibirAlertaSiem(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
            @Valid @RequestBody AlertaSeguridadRequest alerta) {

        if (!validarSecret(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Secret del webhook invalido"));
        }

        Incidente incidente = alertaWebMapper.aDominio(alerta);
        incidente.setId(UUID.randomUUID().toString());
        clasificarIncidenteUseCase.procesarNuevoIncidenteAsync(incidente);

        return ResponseEntity.accepted()
                .body(Map.of("id", incidente.getId(), "estado", "EN_PROCESAMIENTO"));
    }

    private boolean validarSecret(String secret) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return true;
        }
        return webhookSecret.equals(secret);
    }
}