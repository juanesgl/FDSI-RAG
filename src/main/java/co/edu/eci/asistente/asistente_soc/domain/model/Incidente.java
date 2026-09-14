package co.edu.eci.asistente.asistente_soc.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder

public class Incidente {

    private String id;
    private LocalDateTime fechaRecepcion;

    private String descripcion;
    private String ip;
    private String usuario;
    private String sistema;
    private String impacto;

    private String tipo;
    private String severidad;
    private String accionPropuesta;

    private EstadoIncidente estado;

    public void marcarParaAprobacion(String tipo, String severidad, String accion) {

        this.tipo = tipo;
        this.severidad = severidad;
        this.accionPropuesta = accion;
        this.estado = EstadoIncidente.ESPERANDO_APROBACION_HUMANA;

    }

}
