package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.BioLinkItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BioLinkItemRepository extends JpaRepository<BioLinkItem, Long> {

    List<BioLinkItem> findByBioLinkIdOrderByOrdemAsc(Long bioLinkId);

    List<BioLinkItem> findByBioLinkIdAndAtivoTrueOrderByOrdemAsc(Long bioLinkId);

    Optional<BioLinkItem> findByIdAndBioLinkId(Long id, Long bioLinkId);
}
