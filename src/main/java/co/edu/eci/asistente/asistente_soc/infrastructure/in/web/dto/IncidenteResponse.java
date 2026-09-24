package co.edu.eci.asistente.asistente_soc.infrastructure.in.web.dto;

import co.edu.eci.asistente.asistente_soc.domain.model.EstadoIncidente;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Representacion de un incidente ya registrado y, si aplica, ya triado por el "
        + "asistente de IA.")
public record IncidenteResponse(

        @Schema(description = "Identificador interno del incidente.")
        String id,

        @Schema(description = "Fecha y hora en que el incidente quedo registrado en el sistema.")
        LocalDateTime fechaRecepcion,

        @Schema(description = "Descripcion original de la alerta que origino el incidente.")
        String descripcion,

        @Schema(description = "Sistema o agente afectado.")
        String sistema,

        @Schema(description = "Usuario involucrado, si aplica.")
        String usuario,

        @Schema(description = "IP de origen, si aplica.")
        String ip,

        @Schema(description = "Impacto estimado, reportado por la fuente o ajustado por la IA.")
        String impacto,

        @Schema(description = "Tipo de incidente clasificado por la IA (p. ej. phishing, malware).")
        String tipo,

        @Schema(description = "Severidad asignada por la IA.")
        String severidad,

        @Schema(description = "Accion de contencion propuesta por la IA, pendiente de aprobacion humana.")
        String accionPropuesta,

        @Schema(description = "Accion de contencion especifica y ejecutable propuesta por la IA.")
        String comandoEjecucion,

        @Schema(description = "Sistema o servicio objetivo de la contencion.")
        String sistemaAfectado,

        @Schema(description = "IP que la contencion propone bloquear, si aplica.")
        String ipBloqueada,

        @Schema(description = "Id original de la alerta en el SIEM de origen (trazabilidad).")
        String idAlertaOrigen,

        @Schema(description = "Fecha y hora original del evento en el SIEM (ISO-8601 con offset).")
        LocalDateTime timestampOrigen,

        @Schema(description = "Estado actual del incidente dentro de su ciclo de vida.")
        EstadoIncidente estado

) {
}
