package co.edu.eci.asistente.asistente_soc.infrastructure.out.persistence;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidentes")
@Data
public class IncidenteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private LocalDateTime fechaRecepcion;

    @Column(columnDefinition = "TEXT")
    private String descripcion;
    private String ip;
    private String usuario;
    private String sistema;
    private String impacto;

    private String tipo;
    private String severidad;

    @Column(columnDefinition = "TEXT")
    private String accionPropuesta;

    private String estado;
}