package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.Assinatura;
import br.net.convertix.gestor.enums.StatusAssinatura;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssinaturaRepository extends JpaRepository<Assinatura, Long>, JpaSpecificationExecutor<Assinatura> {

    Optional<Assinatura> findByAsaasSubscriptionId(String asaasSubscriptionId);

    List<Assinatura> findByClienteIdOrderByCreatedAtDesc(Long clienteId);

    List<Assinatura> findBySiteIdIn(Collection<Long> siteIds);

    List<Assinatura> findByAplicativoMobileIdIn(Collection<Long> aplicativoMobileIds);

    boolean existsByAplicativoMobileIdAndStatus(Long aplicativoMobileId, StatusAssinatura status);

    boolean existsByAplicativoMobileIdAndStatusAndIdNot(
            Long aplicativoMobileId, StatusAssinatura status, Long id);

    boolean existsByPlanoId(Long planoId);

    Optional<Assinatura> findFirstByClienteIdAndStatusOrderByCreatedAtDesc(Long clienteId, StatusAssinatura status);

    Optional<Assinatura> findFirstByStatusOrderByCreatedAtDesc(StatusAssinatura status);

    boolean existsByClienteIdAndStatus(Long clienteId, StatusAssinatura status);

    @Query("SELECT COUNT(a) FROM Assinatura a WHERE (:clienteId IS NULL OR a.cliente.id = :clienteId) AND a.status = :status")
    long contarPorClienteEStatus(@Param("clienteId") Long clienteId, @Param("status") StatusAssinatura status);

    @Query("SELECT a.status, COUNT(a) FROM Assinatura a WHERE (:clienteId IS NULL OR a.cliente.id = :clienteId) GROUP BY a.status")
    List<Object[]> contarAgrupadoPorStatus(@Param("clienteId") Long clienteId);

    @Query("SELECT a.ciclo, COUNT(a) FROM Assinatura a WHERE (:clienteId IS NULL OR a.cliente.id = :clienteId) GROUP BY a.ciclo")
    List<Object[]> contarAgrupadoPorCiclo(@Param("clienteId") Long clienteId);

    @Query("""
            SELECT a.ciclo, COALESCE(SUM(a.valor), 0)
            FROM Assinatura a
            WHERE (:clienteId IS NULL OR a.cliente.id = :clienteId)
              AND a.status = :status
            GROUP BY a.ciclo
            """)
    List<Object[]> somarValorPorCiclo(@Param("clienteId") Long clienteId, @Param("status") StatusAssinatura status);

    @Query("""
            SELECT YEAR(a.createdAt), MONTH(a.createdAt), COUNT(a)
            FROM Assinatura a
            WHERE (:clienteId IS NULL OR a.cliente.id = :clienteId)
              AND a.createdAt >= :inicio
            GROUP BY YEAR(a.createdAt), MONTH(a.createdAt)
            ORDER BY YEAR(a.createdAt), MONTH(a.createdAt)
            """)
    List<Object[]> contarNovasPorMes(@Param("clienteId") Long clienteId, @Param("inicio") LocalDateTime inicio);

    @Query("""
            SELECT DISTINCT a FROM Assinatura a
            LEFT JOIN FETCH a.cliente
            LEFT JOIN FETCH a.site
            LEFT JOIN FETCH a.aplicativoMobile
            WHERE (:clienteId IS NULL OR a.cliente.id = :clienteId)
              AND a.status = :status
            """)
    List<Assinatura> findPorStatusComProduto(
            @Param("clienteId") Long clienteId,
            @Param("status") StatusAssinatura status);

    @Query("""
            SELECT a FROM Assinatura a JOIN FETCH a.cliente
            WHERE (:clienteId IS NULL OR a.cliente.id = :clienteId)
              AND a.status = :status
            ORDER BY a.createdAt DESC
            """)
    List<Assinatura> findPorStatus(@Param("clienteId") Long clienteId, @Param("status") StatusAssinatura status, Pageable pageable);

    @Query("""
            SELECT a FROM Assinatura a JOIN FETCH a.cliente
            WHERE (:clienteId IS NULL OR a.cliente.id = :clienteId)
              AND a.status = :status
              AND a.proximaCobranca IS NOT NULL
              AND a.proximaCobranca BETWEEN :inicio AND :fim
            ORDER BY a.proximaCobranca ASC
            """)
    List<Assinatura> findVencendoEntre(
            @Param("clienteId") Long clienteId,
            @Param("status") StatusAssinatura status,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim,
            Pageable pageable);

    @Query("""
            SELECT a FROM Assinatura a JOIN FETCH a.cliente
            WHERE (:clienteId IS NULL OR a.cliente.id = :clienteId)
            ORDER BY a.createdAt DESC
            """)
    List<Assinatura> findRecentes(@Param("clienteId") Long clienteId, Pageable pageable);
}
