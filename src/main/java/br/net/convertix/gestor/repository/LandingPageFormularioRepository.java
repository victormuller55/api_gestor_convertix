package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.LandingPageFormulario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LandingPageFormularioRepository extends JpaRepository<LandingPageFormulario, Long> {

    List<LandingPageFormulario> findByLandingPageIdOrderByNomeAsc(Long landingPageId);

    Optional<LandingPageFormulario> findByIdAndLandingPageId(Long id, Long landingPageId);
}
