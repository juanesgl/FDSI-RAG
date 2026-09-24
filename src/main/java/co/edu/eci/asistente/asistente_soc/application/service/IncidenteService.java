package co.edu.eci.asistente.asistente_soc.application.service;

import co.edu.eci.asistente.asistente_soc.domain.exception.IncidenteNoEncontradoException;
import co.edu.eci.asistente.asistente_soc.domain.model.EstadoIncidente;
import co.edu.eci.asistente.asistente_soc.domain.model.Incidente;
import co.edu.eci.asistente.asistente_soc.domain.ports.in.ClasificarIncidenteUseCase;
import co.edu.eci.asistente.asistente_soc.domain.ports.out.AsistenteIaPort;
import co.edu.eci.asistente.asistente_soc.domain.ports.out.IncidenteRepositoryPort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidenteService implements ClasificarIncidenteUseCase {

    private final IncidenteRepositoryPort repositoryPort;
    private final AsistenteIaPort iaPort;

    public IncidenteService(IncidenteRepositoryPort repositoryPort, AsistenteIaPort iaPort) {
        this.repositoryPort = repositoryPort;
        this.iaPort = iaPort;
    }

    @Override
    @Transactional
    public Incidente procesarNuevoIncidente(Incidente incidenteCrudo) {
        Incidente incidenteGuardado = registrarIncidente(incidenteCrudo);
        incidenteGuardado.setEstado(EstadoIncidente.EN_TRIAJE_IA);
        iaPort.analizarYProponerContencion(incidenteGuardado);
        return repositoryPort.actualizar(incidenteGuardado);
    }

    @Override
    @Transactional
    public Incidente registrarIncidente(Incidente incidenteCrudo) {
        incidenteCrudo.setEstado(EstadoIncidente.NUEVO);
        return repositoryPort.guardarNuevo(incidenteCrudo);
    }

    @Override
    @Async
    @Transactional
    public void procesarIncidenteAsync(Incidente incidente) {
        incidente.setEstado(EstadoIncidente.EN_TRIAJE_IA);
        iaPort.analizarYProponerContencion(incidente);
        repositoryPort.actualizar(incidente);
    }

    @Override
    @Transactional
    public Incidente registrarDecisionHumana(String incidenteId, boolean aprobado) {
        Incidente incidente = repositoryPort.buscarPorId(incidenteId)
                .orElseThrow(() -> new IncidenteNoEncontradoException(incidenteId));

        if (aprobado) {
            incidente.setEstado(EstadoIncidente.APROBADO);
        } else {
            incidente.setEstado(EstadoIncidente.RECHAZADO);
        }
        return repositoryPort.actualizar(incidente);
    }
}