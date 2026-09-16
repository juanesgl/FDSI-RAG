package co.edu.eci.asistente.asistente_soc.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Schema(description = "Alerta de seguridad cruda, tal como la reportaria un sistema de deteccion "
        + "(p. ej. Wazuh) o el simulador de alertas del equipo.")
public record AlertaSeguridadRequest(

        @Schema(description = "Fecha y hora en que se detecto el evento. Si no se envia, se usa la hora "
                + "de recepcion del servidor.", example = "2026-09-15T08:30:00")
        LocalDateTime fechaDeteccion,

        @NotBlank(message = "La descripcion de la alerta es obligatoria")
        @Schema(description = "Descripcion del evento generado por la regla de deteccion.",
                example = "Multiples intentos fallidos de autenticacion SSH")
        String descripcion,

        @NotBlank(message = "El sistema o agente afectado es obligatorio")
        @Schema(description = "Nombre del sistema, host o agente donde se origino la alerta.",
                example = "srv-web-01")
        String sistemaAfectado,

        @Schema(description = "Usuario involucrado en el evento, si aplica.", example = "jvalderrama")
        String usuarioAfectado,

        @Schema(description = "Direccion IP de origen del evento, si aplica.", example = "192.168.10.55")
        String ipOrigen,

        @NotBlank(message = "El impacto estimado es obligatorio")
        @Schema(description = "Impacto estimado inicial reportado por la fuente de la alerta.",
                example = "alto")
        String impactoEstimado

) {
}
