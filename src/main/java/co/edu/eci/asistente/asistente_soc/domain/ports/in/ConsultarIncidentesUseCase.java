package co.edu.eci.asistente.asistente_soc.domain.ports.in;

import co.edu.eci.asistente.asistente_soc.domain.model.Incidente;

import java.util.List;

/**
 * Puerto de entrada para consultar incidentes ya registrados. Se mantiene separado de
 * {@link ClasificarIncidenteUseCase} (que modifica el estado del incidente) porque esta
 * responsabilidad es solo de lectura: listar los incidentes y obtener uno por su id para
 * que el analista revise la propuesta antes de decidir.
 */
public interface ConsultarIncidentesUseCase {

    List<Incidente> listarIncidentes();

    Incidente obtenerIncidente(String incidenteId);
}
