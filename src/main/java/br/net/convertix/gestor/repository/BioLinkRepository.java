package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.BioLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface BioLinkRepository extends JpaRepository<BioLink, Long>, JpaSpecificationExecutor<BioLink> {

    Optional<BioLink> findBySiteId(Long siteId);

    boolean existsBySiteId(Long siteId);

    boolean existsBySiteIdAndIdNot(Long siteId, Long id);
}
