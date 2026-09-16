package co.edu.eci.asistente.asistente_soc.application.service;

import co.edu.eci.asistente.asistente_soc.domain.model.EstadoIncidente;
import co.edu.eci.asistente.asistente_soc.domain.model.Incidente;
import co.edu.eci.asistente.asistente_soc.domain.ports.in.ClasificarIncidenteUseCase;
import co.edu.eci.asistente.asistente_soc.domain.ports.out.AsistenteIaPort;
import co.edu.eci.asistente.asistente_soc.domain.ports.out.IncidenteRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class IncidenteService implements ClasificarIncidenteUseCase {

    private final IncidenteRepositoryPort repositoryPort;
    private final AsistenteIaPort iaPort;

    public IncidenteService(IncidenteRepositoryPort repositoryPort, AsistenteIaPort iaPort) {
        this.repositoryPort = repositoryPort;
        this.iaPort = iaPort;
    }

    @Override
    public Incidente procesarNuevoIncidente(Incidente incidenteCrudo) {
        incidenteCrudo.setEstado(EstadoIncidente.NUEVO);

        Incidente incidenteGuardado = repositoryPort.guardar(incidenteCrudo);
        incidenteGuardado.setEstado(EstadoIncidente.EN_TRIAJE_IA);
        iaPort.analizarYProponerContencion(incidenteGuardado);

        return repositoryPort.guardar(incidenteGuardado);
    }

    @Override
    public Incidente registrarDecisionHumana(String incidenteId, boolean aprobado) {
        Incidente incidente = repositoryPort.buscarPorId(incidenteId)
                .orElseThrow(() -> new IllegalArgumentException("Incidente no encontrado en BD"));

        if (aprobado) {
            incidente.setEstado(EstadoIncidente.APROBADO);
        } else {
            incidente.setEstado(EstadoIncidente.RECHAZADO);
        }
        return repositoryPort.guardar(incidente);
    }
}