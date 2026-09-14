package co.edu.eci.asistente.asistente_soc.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidenteJpaRepository extends JpaRepository<IncidenteEntity, String> {
}