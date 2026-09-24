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

    private String comandoEjecucion;
    private String sistemaAfectado;
    private String ipBloqueada;

    private String idAlertaOrigen;
    private LocalDateTime timestampOrigen;

    private EstadoIncidente estado;

    public void marcarParaAprobacion(String tipo, String severidad, String accion) {
        marcarParaAprobacion(tipo, severidad, accion, null, null, null);
    }

    public void marcarParaAprobacion(String tipo, String severidad, String accion,
                                     String comandoEjecucion, String sistemaAfectado,
                                     String ipBloqueada) {

        this.tipo = tipo;
        this.severidad = severidad;
        this.accionPropuesta = accion;
        this.comandoEjecucion = comandoEjecucion;
        this.sistemaAfectado = sistemaAfectado;
        this.ipBloqueada = ipBloqueada;
        this.estado = EstadoIncidente.ESPERANDO_APROBACION_HUMANA;

    }

}
