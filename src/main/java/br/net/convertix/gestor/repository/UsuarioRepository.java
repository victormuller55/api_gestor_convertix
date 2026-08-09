package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("""
            SELECT COUNT(u) FROM Usuario u
            WHERE :clienteId IS NULL
               OR u.id IN (SELECT c.usuario.id FROM Cliente c WHERE c.id = :clienteId AND c.usuario IS NOT NULL)
            """)
    long contarPorEscopo(@Param("clienteId") Long clienteId);

    @Query("""
            SELECT COUNT(u) FROM Usuario u
            WHERE u.ativo = true
              AND (
                  :clienteId IS NULL
                  OR u.id IN (SELECT c.usuario.id FROM Cliente c WHERE c.id = :clienteId AND c.usuario IS NOT NULL)
              )
            """)
    long contarAtivosPorEscopo(@Param("clienteId") Long clienteId);
}
