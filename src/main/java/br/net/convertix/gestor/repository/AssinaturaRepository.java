package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.Assinatura;
import br.net.convertix.gestor.enums.StatusAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssinaturaRepository extends JpaRepository<Assinatura, Long>, JpaSpecificationExecutor<Assinatura> {

    Optional<Assinatura> findByAsaasSubscriptionId(String asaasSubscriptionId);

    List<Assinatura> findByClienteIdOrderByCreatedAtDesc(Long clienteId);

    List<Assinatura> findBySiteIdIn(Collection<Long> siteIds);

    Optional<Assinatura> findFirstByClienteIdAndStatusOrderByCreatedAtDesc(Long clienteId, StatusAssinatura status);

    Optional<Assinatura> findFirstByStatusOrderByCreatedAtDesc(StatusAssinatura status);

    boolean existsByClienteIdAndStatus(Long clienteId, StatusAssinatura status);
}
