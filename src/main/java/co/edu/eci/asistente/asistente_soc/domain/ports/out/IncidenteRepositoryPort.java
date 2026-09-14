package co.edu.eci.asistente.asistente_soc.domain.ports.out;

import co.edu.eci.asistente.asistente_soc.domain.model.Incidente;

import java.util.Optional;

public interface IncidenteRepositoryPort {

    Incidente guardar(Incidente incidente);
    Optional<Incidente> buscarPorId(String id);
}
