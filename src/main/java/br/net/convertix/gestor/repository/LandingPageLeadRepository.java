package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.LandingPageLead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LandingPageLeadRepository extends JpaRepository<LandingPageLead, Long> {

    List<LandingPageLead> findByLandingPageIdOrderByCreatedAtDesc(Long landingPageId);

    List<LandingPageLead> findByFormularioIdOrderByCreatedAtDesc(Long formularioId);

    Optional<LandingPageLead> findByIdAndLandingPageId(Long id, Long landingPageId);
}
