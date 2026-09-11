package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.AplicativoMobile;
import br.net.convertix.gestor.enums.StatusAplicativoMobile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AplicativoMobileRepository
        extends JpaRepository<AplicativoMobile, Long>, JpaSpecificationExecutor<AplicativoMobile> {

    List<AplicativoMobile> findByClienteId(Long clienteId);

    @Query("SELECT COUNT(a) FROM AplicativoMobile a WHERE (:clienteId IS NULL OR a.cliente.id = :clienteId)")
    long contarPorCliente(@Param("clienteId") Long clienteId);

    @Query("""
            SELECT COUNT(a)
            FROM AplicativoMobile a
            WHERE (:clienteId IS NULL OR a.cliente.id = :clienteId)
              AND a.status = :status
            """)
    long contarPorClienteEStatus(
            @Param("clienteId") Long clienteId,
            @Param("status") StatusAplicativoMobile status);

    @Query("""
            SELECT a.status, COUNT(a)
            FROM AplicativoMobile a
            WHERE (:clienteId IS NULL OR a.cliente.id = :clienteId)
            GROUP BY a.status
            """)
    List<Object[]> contarAgrupadoPorStatus(@Param("clienteId") Long clienteId);

    @Query("""
            SELECT YEAR(a.createdAt), MONTH(a.createdAt), COUNT(a)
            FROM AplicativoMobile a
            WHERE (:clienteId IS NULL OR a.cliente.id = :clienteId)
              AND a.createdAt >= :inicio
            GROUP BY YEAR(a.createdAt), MONTH(a.createdAt)
            ORDER BY YEAR(a.createdAt), MONTH(a.createdAt)
            """)
    List<Object[]> contarNovosPorMes(
            @Param("clienteId") Long clienteId,
            @Param("inicio") LocalDateTime inicio);

    @Query("""
            SELECT a FROM AplicativoMobile a JOIN FETCH a.cliente
            WHERE (:clienteId IS NULL OR a.cliente.id = :clienteId)
            ORDER BY a.createdAt DESC
            """)
    List<AplicativoMobile> findRecentes(@Param("clienteId") Long clienteId, Pageable pageable);
}
