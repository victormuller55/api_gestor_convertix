package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.LandingPageLeadValor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LandingPageLeadValorRepository extends JpaRepository<LandingPageLeadValor, Long> {

    List<LandingPageLeadValor> findByLeadId(Long leadId);

    List<LandingPageLeadValor> findByCampoId(Long campoId);
}
