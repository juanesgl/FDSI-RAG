package co.edu.eci.asistente.asistente_soc.domain.ports.out;

import co.edu.eci.asistente.asistente_soc.domain.model.Incidente;

public interface AsistenteIaPort {
    void analizarYProponerContencion(Incidente incidente);
}
