package co.edu.eci.asistente.asistente_soc.infrastructure.out.ia;

import java.util.List;

/**
 * Contrato de salida estructurada del LLM (Mision 3).
 * Spring AI lo deserializa con BeanOutputConverter a partir del JSON schema generado
 * desde este record: el modelo queda obligado a devolver exactamente esta forma,
 * sin parseo manual ni texto conversacional.
 */
public record RespuestaLlm(
        String tipo,
        String severidad,
        String justificacion,
        List<AccionContencion> accionesRecomendadas,
        String comandoEjecucion,
        String sistemaAfectado,
        String ipBloqueada,
        Boolean informacionInsuficiente,
        String preguntaSeguimiento
) {

    /**
     * Accion de contencion especifica y ejecutable (no generica): comando concreto,
     * sistema/servicio objetivo y parametro exacto (IP, usuario, host, puerto).
     */
    public record AccionContencion(
            String accion,
            String objetivo,
            String comando
    ) {
    }
}