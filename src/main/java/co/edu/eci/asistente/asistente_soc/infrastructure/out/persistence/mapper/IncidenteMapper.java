package co.edu.eci.asistente.asistente_soc.infrastructure.out.persistence.mapper;

import co.edu.eci.asistente.asistente_soc.domain.model.Incidente;
import co.edu.eci.asistente.asistente_soc.infrastructure.out.persistence.IncidenteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface IncidenteMapper {

    IncidenteEntity toEntity(Incidente domain);

    Incidente toDomain(IncidenteEntity entity);
}