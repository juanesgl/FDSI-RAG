package co.edu.eci.asistente.asistente_soc.infrastructure.out.persistence;

import co.edu.eci.asistente.asistente_soc.domain.model.Incidente;
import co.edu.eci.asistente.asistente_soc.domain.ports.out.IncidenteRepositoryPort;
import co.edu.eci.asistente.asistente_soc.infrastructure.out.persistence.mapper.IncidenteMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class IncidentePersistenceAdapter implements IncidenteRepositoryPort {

    private final IncidenteJpaRepository jpaRepository;
    private final IncidenteMapper mapper;

    public IncidentePersistenceAdapter(IncidenteJpaRepository jpaRepository, IncidenteMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Incidente guardar(Incidente incidente) {
        IncidenteEntity entity = mapper.toEntity(incidente);
        IncidenteEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Incidente> buscarPorId(String id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }
}