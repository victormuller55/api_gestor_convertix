package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.LandingPageCampo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LandingPageCampoRepository extends JpaRepository<LandingPageCampo, Long> {

    List<LandingPageCampo> findByFormularioIdOrderByOrdemAsc(Long formularioId);

    List<LandingPageCampo> findByFormularioIdAndAtivoTrueOrderByOrdemAsc(Long formularioId);

    Optional<LandingPageCampo> findByIdAndFormularioId(Long id, Long formularioId);

    Optional<LandingPageCampo> findByFormularioIdAndNomeInterno(Long formularioId, String nomeInterno);

    boolean existsByFormularioIdAndNomeInterno(Long formularioId, String nomeInterno);
}
