package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.Cliente;
import br.net.convertix.gestor.enums.StatusPagamento;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    Optional<Cliente> findByUsuarioId(Long usuarioId);

    boolean existsByUsuarioId(Long usuarioId);

    boolean existsByUsuarioIdAndIdNot(Long usuarioId, Long id);

    boolean existsByDocumento(String documento);

    boolean existsByDocumentoAndIdNot(String documento, Long id);

    Optional<Cliente> findByAsaasCustomerId(String asaasCustomerId);

    @Query("SELECT COUNT(c) FROM Cliente c WHERE (:clienteId IS NULL OR c.id = :clienteId)")
    long contarPorEscopo(@Param("clienteId") Long clienteId);

    @Query("""
            SELECT YEAR(c.createdAt), MONTH(c.createdAt), COUNT(c)
            FROM Cliente c
            WHERE (:clienteId IS NULL OR c.id = :clienteId)
              AND c.createdAt >= :inicio
            GROUP BY YEAR(c.createdAt), MONTH(c.createdAt)
            ORDER BY YEAR(c.createdAt), MONTH(c.createdAt)
            """)
    List<Object[]> contarNovosPorMes(@Param("clienteId") Long clienteId, @Param("inicio") LocalDateTime inicio);

    @Query("""
            SELECT COUNT(DISTINCT c.id) FROM Cliente c
            WHERE (:clienteId IS NULL OR c.id = :clienteId)
              AND EXISTS (SELECT 1 FROM Site s WHERE s.cliente = c)
            """)
    long contarComSite(@Param("clienteId") Long clienteId);

    @Query("""
            SELECT COUNT(DISTINCT c.id) FROM Cliente c
            WHERE (:clienteId IS NULL OR c.id = :clienteId)
              AND EXISTS (SELECT 1 FROM Assinatura a WHERE a.cliente = c)
            """)
    long contarComAssinatura(@Param("clienteId") Long clienteId);

    @Query("""
            SELECT COUNT(DISTINCT c.id) FROM Cliente c
            WHERE (:clienteId IS NULL OR c.id = :clienteId)
              AND EXISTS (
                  SELECT 1 FROM Pagamento p
                  WHERE p.cliente = c AND p.status IN :statuses
              )
            """)
    long contarComPagamentoPago(@Param("clienteId") Long clienteId, @Param("statuses") List<StatusPagamento> statuses);

    @Query("""
            SELECT c FROM Cliente c
            WHERE (:clienteId IS NULL OR c.id = :clienteId)
            ORDER BY c.createdAt DESC
            """)
    List<Cliente> findRecentes(@Param("clienteId") Long clienteId, Pageable pageable);
}
