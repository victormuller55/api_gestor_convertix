package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.Pagamento;
import br.net.convertix.gestor.enums.FormaPagamento;
import br.net.convertix.gestor.enums.StatusPagamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long>, JpaSpecificationExecutor<Pagamento> {

    Optional<Pagamento> findByAsaasPaymentId(String asaasPaymentId);

    @Query("SELECT p FROM Pagamento p WHERE p.asaasPaymentId IS NOT NULL "
            + "AND (:clienteId IS NULL OR p.cliente.id = :clienteId)")
    List<Pagamento> findComAsaasPaymentId(@Param("clienteId") Long clienteId);

    List<Pagamento> findTop10ByClienteIdOrderByCreatedAtDesc(Long clienteId);

    List<Pagamento> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p WHERE (:clienteId IS NULL OR p.cliente.id = :clienteId) AND p.status IN :statuses")
    BigDecimal somarPorStatus(@Param("clienteId") Long clienteId, @Param("statuses") List<StatusPagamento> statuses);

    long countByClienteId(Long clienteId);

    long countByClienteIdAndStatus(Long clienteId, StatusPagamento status);

    long countByStatus(StatusPagamento status);

    Page<Pagamento> findByClienteId(Long clienteId, Pageable pageable);

    List<Pagamento> findByAssinaturaIdOrderByCreatedAtDesc(Long assinaturaId);

    boolean existsByAssinaturaIdAndDataVencimento(Long assinaturaId, LocalDate dataVencimento);

    boolean existsByAssinaturaIdAndStatusIn(Long assinaturaId, List<StatusPagamento> statuses);

    boolean existsByClienteIdAndFormaPagamento(Long clienteId, FormaPagamento formaPagamento);
}
