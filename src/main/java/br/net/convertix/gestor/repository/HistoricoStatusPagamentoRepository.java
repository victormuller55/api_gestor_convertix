package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.HistoricoStatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoStatusPagamentoRepository extends JpaRepository<HistoricoStatusPagamento, Long> {

    List<HistoricoStatusPagamento> findByPagamentoIdOrderByCreatedAtAsc(Long pagamentoId);
}
