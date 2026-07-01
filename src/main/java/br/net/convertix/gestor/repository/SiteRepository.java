package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SiteRepository extends JpaRepository<Site, Long>, JpaSpecificationExecutor<Site> {

    List<Site> findByClienteId(Long clienteId);

    boolean existsBySubdominio(String subdominio);

    boolean existsBySubdominioAndIdNot(String subdominio, Long id);
}
