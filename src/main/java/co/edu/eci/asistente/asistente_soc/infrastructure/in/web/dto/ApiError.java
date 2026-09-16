package co.edu.eci.asistente.asistente_soc.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Formato estandar de error de la API.")
public record ApiError(

        @Schema(description = "Momento en que ocurrio el error.")
        LocalDateTime momento,

        @Schema(description = "Codigo de estado HTTP asociado.")
        int status,

        @Schema(description = "Mensaje general del error.")
        String mensaje,

        @Schema(description = "Detalle de errores especificos, por ejemplo por campo invalido.")
        List<String> detalles

) {
}
