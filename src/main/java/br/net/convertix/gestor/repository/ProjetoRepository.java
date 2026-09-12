package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.Projeto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProjetoRepository extends JpaRepository<Projeto, Long>, JpaSpecificationExecutor<Projeto> {

    List<Projeto> findByClienteId(Long clienteId);

    List<Projeto> findBySiteId(Long siteId);

    List<Projeto> findByAplicativoMobileId(Long aplicativoMobileId);
}
