package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.LandingPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface LandingPageRepository extends JpaRepository<LandingPage, Long>, JpaSpecificationExecutor<LandingPage> {

    Optional<LandingPage> findBySlug(String slug);

    Optional<LandingPage> findBySiteId(Long siteId);

    boolean existsBySiteId(Long siteId);

    boolean existsBySiteIdAndIdNot(Long siteId, Long id);
}
