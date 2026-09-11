package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.AplicativoMobile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AplicativoMobileRepository
        extends JpaRepository<AplicativoMobile, Long>, JpaSpecificationExecutor<AplicativoMobile> {

    List<AplicativoMobile> findByClienteId(Long clienteId);
}
