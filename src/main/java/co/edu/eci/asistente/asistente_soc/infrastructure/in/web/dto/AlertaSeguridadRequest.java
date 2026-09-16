package co.edu.eci.asistente.asistente_soc.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Map;

@Schema(description = "Alerta de seguridad cruda con estructura compatible con el contrato de alertas simuladas.")
public record AlertaSeguridadRequest(

        @Schema(
                description = "Identificador de la alerta.",
                example = "esc-01-alert-001"
        )
        String id,

        @Schema(
                description = "Fecha y hora del evento en formato ISO-8601 con offset.",
                example = "2026-09-14T08:30:00-05:00"
        )
        OffsetDateTime timestamp,

        @Schema(description = "Regla que genero la alerta.")
        Rule rule,

        @Schema(description = "Agente que genero la alerta, si aplica.")
        Agent agent,

        @Schema(
                description = "Datos observados por la regla. Debe contener valores simples."
        )
        Map<String, Object> data,

        @Schema(
                description = "Registro original de la alerta.",
                example = "Multiple failed SSH authentication attempts"
        )
        String full_log,

        @Schema(
                description = "Origen o ubicacion de la alerta.",
                example = "auth"
        )
        String location

) {

        public record Rule(

                @Schema(example = "5710")
                String id,

                @Schema(example = "Multiple failed SSH authentication attempts")
                String description,

                @Schema(
                        description = "Nivel de la regla entre 0 y 15.",
                        minimum = "0",
                        maximum = "15",
                        example = "10"
                )
                Integer level,

                java.util.List<String> groups,

                java.util.List<String> mitre,

                Integer firedtimes
        ) {
        }

        public record Agent(

                @Schema(example = "001")
                String id,

                @Schema(example = "srv-web-01")
                String name,

                @Schema(example = "192.168.10.20")
                String ip
        ) {
        }
}