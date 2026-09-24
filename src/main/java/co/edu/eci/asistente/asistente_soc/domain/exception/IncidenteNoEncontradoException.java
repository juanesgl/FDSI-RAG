package co.edu.eci.asistente.asistente_soc.domain.exception;

/**
 * Excepcion de dominio: el incidente solicitado no existe en la base de datos.
 * Se traduce a HTTP 404 en el adaptador de entrada (GlobalExceptionHandler).
 */
public class IncidenteNoEncontradoException extends RuntimeException {

    public IncidenteNoEncontradoException(String incidenteId) {
        super("Incidente no encontrado en BD: " + incidenteId);
    }
}