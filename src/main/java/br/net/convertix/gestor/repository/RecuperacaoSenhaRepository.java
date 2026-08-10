package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.RecuperacaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenha, Long> {

    Optional<RecuperacaoSenha> findFirstByUsuarioIdOrderByEnviadoEmDesc(Long usuarioId);

    void deleteByUsuarioId(Long usuarioId);
}
