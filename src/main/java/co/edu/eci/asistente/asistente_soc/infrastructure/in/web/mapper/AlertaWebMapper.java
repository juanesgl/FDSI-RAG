package co.edu.eci.asistente.asistente_soc.infrastructure.in.web.mapper;

import co.edu.eci.asistente.asistente_soc.domain.model.Incidente;
import co.edu.eci.asistente.asistente_soc.infrastructure.in.web.dto.AlertaSeguridadRequest;
import co.edu.eci.asistente.asistente_soc.infrastructure.in.web.dto.IncidenteResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AlertaWebMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "sistemaAfectado", target = "sistema")
    @Mapping(source = "usuarioAfectado", target = "usuario")
    @Mapping(source = "ipOrigen", target = "ip")
    @Mapping(source = "impactoEstimado", target = "impacto")
    @Mapping(target = "tipo", ignore = true)
    @Mapping(target = "severidad", ignore = true)
    @Mapping(target = "accionPropuesta", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "fechaRecepcion",
            expression = "java(alerta.fechaDeteccion() != null ? alerta.fechaDeteccion() : java.time.LocalDateTime.now())")
    Incidente aDominio(AlertaSeguridadRequest alerta);

    IncidenteResponse aResponse(Incidente incidente);
}
