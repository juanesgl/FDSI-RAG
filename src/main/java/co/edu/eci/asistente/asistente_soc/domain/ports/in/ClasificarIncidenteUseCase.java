package co.edu.eci.asistente.asistente_soc.domain.ports.in;

import co.edu.eci.asistente.asistente_soc.domain.model.Incidente;

public interface ClasificarIncidenteUseCase {
    Incidente procesarNuevoIncidente(Incidente incidenteCrudo);
    void procesarNuevoIncidenteAsync(Incidente incidenteCrudo);
    Incidente registrarDecisionHumana(String incidenteId, boolean aprobado);
}
