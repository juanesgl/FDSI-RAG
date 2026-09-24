package co.edu.eci.asistente.asistente_soc.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "Alerta de seguridad cruda con estructura compatible con el contrato de alertas simuladas.")
public record AlertaSeguridadRequest(

        @NotBlank
        @Schema(
                description = "Identificador de la alerta.",
                example = "esc-01-alert-001"
        )
        String id,

        @NotNull
        @Schema(
                description = "Fecha y hora del evento en formato ISO-8601 con offset.",
                example = "2026-09-14T08:30:00-05:00"
        )
        OffsetDateTime timestamp,

        @NotNull
        @Valid
        @Schema(description = "Regla que genero la alerta.")
        Rule rule,

        @Schema(description = "Agente que genero la alerta, si aplica.")
        Agent agent,

        @NotNull
        @Schema(
                description = "Datos observados por la regla. Debe contener valores simples."
        )
        Map<String, Object> data,

        @NotBlank
        @Schema(
                description = "Registro original de la alerta.",
                example = "Multiple failed SSH authentication attempts"
        )
        String full_log,

        @NotBlank
        @Schema(
                description = "Origen o ubicacion de la alerta.",
                example = "auth"
        )
        String location

) {

        public record Rule(

                @NotBlank
                @Schema(example = "5710")
                String id,

                @NotBlank
                @Schema(example = "Multiple failed SSH authentication attempts")
                String description,

                @Schema(
                        description = "Nivel de la regla entre 0 y 15.",
                        minimum = "0",
                        maximum = "15",
                        example = "10"
                )
                Integer level,

                List<String> groups,

                Mitre mitre,

                Integer firedtimes
        ) {
                public record Mitre(

                        List<String> id,

                        List<String> technique
                ) {
                }
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