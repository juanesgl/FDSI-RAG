package co.edu.eci.asistente.asistente_soc.infrastructure.in.web;

import co.edu.eci.asistente.asistente_soc.domain.model.Incidente;
import co.edu.eci.asistente.asistente_soc.domain.ports.in.ClasificarIncidenteUseCase;
import co.edu.eci.asistente.asistente_soc.domain.ports.in.ConsultarIncidentesUseCase;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alertas")
@Tag(name = "Alertas de seguridad", description = "Punto de entrada oficial para inyectar alertas de "
        + "seguridad (reales o simuladas) al asistente SOC.")
public class AlertaController {

    private final ClasificarIncidenteUseCase clasificarIncidenteUseCase;
    private final ConsultarIncidentesUseCase consultarIncidentesUseCase;
    private final AlertaWebMapper alertaWebMapper;

    public AlertaController(ClasificarIncidenteUseCase clasificarIncidenteUseCase,
                             ConsultarIncidentesUseCase consultarIncidentesUseCase,
                             AlertaWebMapper alertaWebMapper) {
        this.clasificarIncidenteUseCase = clasificarIncidenteUseCase;
        this.consultarIncidentesUseCase = consultarIncidentesUseCase;
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

    @Operation(summary = "Listar los incidentes registrados",
            description = "Devuelve todos los incidentes (del mas reciente al mas antiguo) para que el "
                    + "analista revise las propuestas pendientes, sin importar la fuente que las genero.")
    @ApiResponse(responseCode = "200", description = "Lista de incidentes",
            content = @Content(schema = @Schema(implementation = IncidenteResponse.class)))
    @GetMapping
    public List<IncidenteResponse> listarIncidentes() {
        return consultarIncidentesUseCase.listarIncidentes().stream()
                .map(alertaWebMapper::aResponse)
                .toList();
    }

    @Operation(summary = "Obtener un incidente por su id",
            description = "Devuelve el detalle de un incidente puntual, incluida la propuesta de la IA "
                    + "y su estado dentro del flujo de aprobacion humana.")
    @ApiResponse(responseCode = "200", description = "Incidente encontrado",
            content = @Content(schema = @Schema(implementation = IncidenteResponse.class)))
    @ApiResponse(responseCode = "404", description = "No existe un incidente con ese id")
    @GetMapping("/{incidenteId}")
    public IncidenteResponse obtenerIncidente(@PathVariable String incidenteId) {
        return alertaWebMapper.aResponse(consultarIncidentesUseCase.obtenerIncidente(incidenteId));
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
