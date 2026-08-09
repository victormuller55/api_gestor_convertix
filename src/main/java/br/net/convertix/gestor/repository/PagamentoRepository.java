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
import java.time.LocalDateTime;
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

    @Query("SELECT COUNT(p) FROM Pagamento p WHERE (:clienteId IS NULL OR p.cliente.id = :clienteId)")
    long contarPorCliente(@Param("clienteId") Long clienteId);

    @Query("SELECT COUNT(p) FROM Pagamento p WHERE (:clienteId IS NULL OR p.cliente.id = :clienteId) AND p.status = :status")
    long contarPorClienteEStatus(@Param("clienteId") Long clienteId, @Param("status") StatusPagamento status);

    @Query("SELECT COUNT(p) FROM Pagamento p WHERE (:clienteId IS NULL OR p.cliente.id = :clienteId) AND p.status IN :statuses")
    long contarPorClienteEStatuses(@Param("clienteId") Long clienteId, @Param("statuses") List<StatusPagamento> statuses);

    @Query("""
            SELECT p.status, COUNT(p), COALESCE(SUM(p.valor), 0)
            FROM Pagamento p
            WHERE (:clienteId IS NULL OR p.cliente.id = :clienteId)
            GROUP BY p.status
            """)
    List<Object[]> agregarPorStatus(@Param("clienteId") Long clienteId);

    @Query("""
            SELECT p.formaPagamento, COUNT(p), COALESCE(SUM(p.valor), 0)
            FROM Pagamento p
            WHERE (:clienteId IS NULL OR p.cliente.id = :clienteId)
              AND p.formaPagamento IS NOT NULL
            GROUP BY p.formaPagamento
            """)
    List<Object[]> agregarPorForma(@Param("clienteId") Long clienteId);

    @Query("""
            SELECT COALESCE(SUM(p.valor), 0)
            FROM Pagamento p
            WHERE (:clienteId IS NULL OR p.cliente.id = :clienteId)
              AND p.status IN :statuses
              AND COALESCE(p.dataConfirmacao, p.createdAt) >= :inicio
              AND COALESCE(p.dataConfirmacao, p.createdAt) < :fim
            """)
    BigDecimal somarPagoNoPeriodo(
            @Param("clienteId") Long clienteId,
            @Param("statuses") List<StatusPagamento> statuses,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("""
            SELECT YEAR(p.createdAt), MONTH(p.createdAt),
                   COALESCE(SUM(CASE WHEN p.status IN :pagos THEN p.valor ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN p.status IN :pendentes THEN p.valor ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN p.status IN :pagos THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN p.status IN :pendentes THEN 1 ELSE 0 END), 0)
            FROM Pagamento p
            WHERE (:clienteId IS NULL OR p.cliente.id = :clienteId)
              AND p.createdAt >= :inicio
            GROUP BY YEAR(p.createdAt), MONTH(p.createdAt)
            ORDER BY YEAR(p.createdAt), MONTH(p.createdAt)
            """)
    List<Object[]> agregarReceitaMensal(
            @Param("clienteId") Long clienteId,
            @Param("inicio") LocalDateTime inicio,
            @Param("pagos") List<StatusPagamento> pagos,
            @Param("pendentes") List<StatusPagamento> pendentes);

    @Query("""
            SELECT p.cliente.id, p.cliente.nomeEmpresa, COALESCE(SUM(p.valor), 0), COUNT(p)
            FROM Pagamento p
            WHERE (:clienteId IS NULL OR p.cliente.id = :clienteId)
              AND p.status IN :statuses
            GROUP BY p.cliente.id, p.cliente.nomeEmpresa
            ORDER BY SUM(p.valor) DESC
            """)
    List<Object[]> topClientesPorReceita(
            @Param("clienteId") Long clienteId,
            @Param("statuses") List<StatusPagamento> statuses,
            Pageable pageable);

    @Query("""
            SELECT p FROM Pagamento p JOIN FETCH p.cliente
            WHERE (:clienteId IS NULL OR p.cliente.id = :clienteId)
            ORDER BY p.createdAt DESC
            """)
    List<Pagamento> findRecentes(@Param("clienteId") Long clienteId, Pageable pageable);

    @Query("""
            SELECT p FROM Pagamento p JOIN FETCH p.cliente
            WHERE (:clienteId IS NULL OR p.cliente.id = :clienteId)
              AND p.status = :status
            ORDER BY p.dataVencimento ASC, p.createdAt ASC
            """)
    List<Pagamento> findPorStatus(
            @Param("clienteId") Long clienteId,
            @Param("status") StatusPagamento status,
            Pageable pageable);
}
