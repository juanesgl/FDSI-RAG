package co.edu.eci.asistente.asistente_soc.infrastructure.in.web;

import co.edu.eci.asistente.asistente_soc.domain.model.Incidente;
import co.edu.eci.asistente.asistente_soc.domain.ports.in.ClasificarIncidenteUseCase;
import co.edu.eci.asistente.asistente_soc.infrastructure.in.web.dto.AlertaSeguridadRequest;
import co.edu.eci.asistente.asistente_soc.infrastructure.in.web.dto.IncidenteResponse;
import co.edu.eci.asistente.asistente_soc.infrastructure.in.web.mapper.AlertaWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alertas")
@Tag(name = "Alertas de seguridad", description = "Punto de entrada oficial para inyectar alertas de "
        + "seguridad (reales o simuladas) al asistente SOC.")
public class AlertaController {

    private final ClasificarIncidenteUseCase clasificarIncidenteUseCase;
    private final AlertaWebMapper alertaWebMapper;

    public AlertaController(ClasificarIncidenteUseCase clasificarIncidenteUseCase,
                             AlertaWebMapper alertaWebMapper) {
        this.clasificarIncidenteUseCase = clasificarIncidenteUseCase;
        this.alertaWebMapper = alertaWebMapper;
    }

    @Operation(summary = "Recibir una alerta de seguridad",
            description = "Recibe una alerta de seguridad, la mapea al modelo de dominio y dispara el "
                    + "triaje automatico con IA.")
    @ApiResponse(responseCode = "201", description = "Alerta recibida y encolada para triaje",
            content = @Content(schema = @Schema(implementation = IncidenteResponse.class)))
    @ApiResponse(responseCode = "400", description = "La alerta no cumple con los campos obligatorios")
    @PostMapping
    public ResponseEntity<IncidenteResponse> recibirAlerta(@Valid @RequestBody AlertaSeguridadRequest alerta) {
        Incidente incidenteCrudo = alertaWebMapper.aDominio(alerta);
        Incidente incidenteProcesado = clasificarIncidenteUseCase.procesarNuevoIncidente(incidenteCrudo);
        return ResponseEntity.status(HttpStatus.CREATED).body(alertaWebMapper.aResponse(incidenteProcesado));
    }

    @Operation(summary = "Registrar la decision humana sobre un incidente",
            description = "Permite aprobar o rechazar la accion de contencion propuesta por la IA para "
                    + "un incidente puntual.")
    @ApiResponse(responseCode = "200", description = "Decision registrada",
            content = @Content(schema = @Schema(implementation = IncidenteResponse.class)))
    @ApiResponse(responseCode = "404", description = "No existe un incidente con ese id")
    @PutMapping("/{incidenteId}/decision")
    public ResponseEntity<IncidenteResponse> registrarDecision(@PathVariable String incidenteId,
                                                                @RequestParam boolean aprobado) {
        Incidente incidenteActualizado = clasificarIncidenteUseCase.registrarDecisionHumana(incidenteId, aprobado);
        return ResponseEntity.ok(alertaWebMapper.aResponse(incidenteActualizado));
    }
}
