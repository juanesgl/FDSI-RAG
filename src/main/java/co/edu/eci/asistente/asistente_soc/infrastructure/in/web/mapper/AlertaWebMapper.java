package co.edu.eci.asistente.asistente_soc.infrastructure.in.web.mapper;

import co.edu.eci.asistente.asistente_soc.domain.model.Incidente;
import co.edu.eci.asistente.asistente_soc.infrastructure.in.web.dto.AlertaSeguridadRequest;
import co.edu.eci.asistente.asistente_soc.infrastructure.in.web.dto.IncidenteResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.StringJoiner;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class AlertaWebMapper {

    public Incidente aDominio(AlertaSeguridadRequest alerta) {

        String sistema = obtenerSistema(alerta);
        String usuario = obtenerUsuario(alerta);
        String impacto = obtenerImpacto(alerta);

        return Incidente.builder()
                .id(null)
                .fechaRecepcion(LocalDateTime.now())
                .descripcion(construirDescripcion(alerta))
                .sistema(sistema)
                .usuario(usuario)
                .ip(obtenerIp(alerta))
                .impacto(impacto)
                .idAlertaOrigen(alerta.id())
                .timestampOrigen(alerta.timestamp() != null
                        ? alerta.timestamp().toLocalDateTime()
                        : null)
                .tipo(null)
                .severidad(null)
                .accionPropuesta(null)
                .estado(null)
                .build();
    }

    public abstract IncidenteResponse aResponse(Incidente incidente);

    private String obtenerSistema(AlertaSeguridadRequest alerta) {

        if (alerta.agent() == null) {
            return null;
        }

        if ("000".equals(alerta.agent().id())) {
            return null;
        }

        return alerta.agent().name();
    }

    private String obtenerIp(AlertaSeguridadRequest alerta) {

        if (alerta.agent() == null) {
            return null;
        }

        return alerta.agent().ip();
    }

    private String obtenerUsuario(AlertaSeguridadRequest alerta) {

        if (alerta.data() == null) {
            return null;
        }

        Object dstuser = alerta.data().get("dstuser");

        if (dstuser != null && !dstuser.toString().isBlank()) {
            return dstuser.toString();
        }

        Object srcuser = alerta.data().get("srcuser");

        if (srcuser != null && !srcuser.toString().isBlank()) {
            return srcuser.toString();
        }

        return null;
    }

    private String obtenerImpacto(AlertaSeguridadRequest alerta) {

        if (alerta.data() == null) {
            return null;
        }

        Object impacto = alerta.data().get("impacto_reportado");

        if (impacto == null) {
            return null;
        }

        String valor = impacto.toString();

        return valor.isBlank() ? null : valor;
    }

    private String construirDescripcion(AlertaSeguridadRequest alerta) {

        StringBuilder descripcion = new StringBuilder();

        descripcion.append("Alerta de monitoreo recibida desde ")
                .append(valorSeguro(alerta.location()))
                .append(".\n");

        if (alerta.rule() != null) {
            descripcion.append("Evento detectado: ")
                    .append(valorSeguro(alerta.rule().description()))
                    .append(".\n");
        }

        descripcion.append("Registro original: ")
                .append(valorSeguro(alerta.full_log()))
                .append("\n");

        String datosObservados = serializarData(alerta.data());

        if (!datosObservados.isBlank()) {
            descripcion.append("Datos observados: ")
                    .append(datosObservados)
                    .append("\n");
        }

        descripcion.append("Hora del evento: ")
                .append(alerta.timestamp() != null
                        ? alerta.timestamp().toString()
                        : "no disponible");

        return descripcion.toString();
    }

    private String serializarData(Map<String, Object> data) {

        if (data == null || data.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner("; ");

        data.forEach((key, value) -> {

            if ("impacto_reportado".equals(key)) {
                return;
            }

            if (value == null) {
                return;
            }

            String valor = value.toString();

            if (valor.isBlank()) {
                return;
            }

            String claveFormateada = key.replace("_", " ");

            if (value instanceof Boolean booleanValue) {
                valor = booleanValue ? "sí" : "no";
            }

            joiner.add(claveFormateada + "=" + valor);
        });

        return joiner.toString();
    }

    private String valorSeguro(String valor) {
        return valor != null ? valor : "no disponible";
    }
}