package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.Site;
import br.net.convertix.gestor.enums.StatusSite;
import br.net.convertix.gestor.enums.TipoSite;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SiteRepository extends JpaRepository<Site, Long>, JpaSpecificationExecutor<Site> {

    List<Site> findByClienteId(Long clienteId);

    boolean existsBySubdominio(String subdominio);

    boolean existsBySubdominioAndIdNot(String subdominio, Long id);

    @Query("SELECT COUNT(s) FROM Site s WHERE (:clienteId IS NULL OR s.cliente.id = :clienteId)")
    long contarPorCliente(@Param("clienteId") Long clienteId);

    @Query("SELECT COUNT(s) FROM Site s WHERE (:clienteId IS NULL OR s.cliente.id = :clienteId) AND s.status = :status")
    long contarPorClienteEStatus(@Param("clienteId") Long clienteId, @Param("status") StatusSite status);

    @Query("SELECT COUNT(s) FROM Site s WHERE (:clienteId IS NULL OR s.cliente.id = :clienteId) AND s.tipo = :tipo")
    long contarPorClienteETipo(@Param("clienteId") Long clienteId, @Param("tipo") TipoSite tipo);

    @Query("SELECT s.status, COUNT(s) FROM Site s WHERE (:clienteId IS NULL OR s.cliente.id = :clienteId) GROUP BY s.status")
    List<Object[]> contarAgrupadoPorStatus(@Param("clienteId") Long clienteId);

    @Query("SELECT s.tipo, COUNT(s) FROM Site s WHERE (:clienteId IS NULL OR s.cliente.id = :clienteId) GROUP BY s.tipo")
    List<Object[]> contarAgrupadoPorTipo(@Param("clienteId") Long clienteId);

    @Query("""
            SELECT YEAR(s.createdAt), MONTH(s.createdAt), COUNT(s)
            FROM Site s
            WHERE (:clienteId IS NULL OR s.cliente.id = :clienteId)
              AND s.createdAt >= :inicio
            GROUP BY YEAR(s.createdAt), MONTH(s.createdAt)
            ORDER BY YEAR(s.createdAt), MONTH(s.createdAt)
            """)
    List<Object[]> contarNovosPorMes(@Param("clienteId") Long clienteId, @Param("inicio") LocalDateTime inicio);

    @Query("""
            SELECT s FROM Site s JOIN FETCH s.cliente
            WHERE (:clienteId IS NULL OR s.cliente.id = :clienteId)
            ORDER BY s.createdAt DESC
            """)
    List<Site> findRecentes(@Param("clienteId") Long clienteId, Pageable pageable);

    @Query("""
            SELECT s FROM Site s JOIN FETCH s.cliente
            WHERE (:clienteId IS NULL OR s.cliente.id = :clienteId)
              AND s.status = :status
            ORDER BY s.updatedAt DESC
            """)
    List<Site> findPorStatus(@Param("clienteId") Long clienteId, @Param("status") StatusSite status, Pageable pageable);
}
