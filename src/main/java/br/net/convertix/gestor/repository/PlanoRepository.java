package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.Plano;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PlanoRepository extends JpaRepository<Plano, Long>, JpaSpecificationExecutor<Plano> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);
}
