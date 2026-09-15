package co.edu.eci.asistente.asistente_soc.infrastructure.in.web;

import co.edu.eci.asistente.asistente_soc.domain.model.Incidente;
import co.edu.eci.asistente.asistente_soc.domain.ports.in.ClasificarIncidenteUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * SOLO para probar el modulo RAG+LLM de forma aislada mientras el Estudiante 3 construye el
 * controlador REST oficial del equipo (el que mapeara las alertas reales, con su propio DTO,
 * validaciones, documentacion OpenAPI, etc). Cuando el suyo este listo, este archivo se puede
 * borrar sin que nada mas dependa de el: el contrato real que el resto del equipo debe usar
 * es ClasificarIncidenteUseCase.
 */
@RestController
public class IncidenteTestController {

    private final ClasificarIncidenteUseCase clasificarIncidenteUseCase;

    public IncidenteTestController(ClasificarIncidenteUseCase clasificarIncidenteUseCase) {
        this.clasificarIncidenteUseCase = clasificarIncidenteUseCase;
    }

    @PostMapping("/internal/test/clasificar")
    public Incidente clasificar(@RequestBody TestIncidenteRequest request) {
        Incidente incidente = Incidente.builder()
                .fechaRecepcion(LocalDateTime.now())
                .descripcion(request.descripcion())
                .sistema(request.sistema())
                .usuario(request.usuario())
                .ip(request.ip())
                .impacto(request.impacto())
                .build();

        return clasificarIncidenteUseCase.procesarNuevoIncidente(incidente);
    }

    public record TestIncidenteRequest(
            String descripcion,
            String sistema,
            String usuario,
            String ip,
            String impacto
    ) {
    }
}
