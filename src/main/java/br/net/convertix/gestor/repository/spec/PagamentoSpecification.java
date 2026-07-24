package br.net.convertix.gestor.repository.spec;

import br.net.convertix.gestor.entity.Pagamento;
import br.net.convertix.gestor.enums.FormaPagamento;
import br.net.convertix.gestor.enums.StatusPagamento;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

@UtilityClass
public class PagamentoSpecification {

    public Specification<Pagamento> comFiltros(
            Long clienteId,
            StatusPagamento status,
            FormaPagamento formaPagamento,
            LocalDateTime dataInicio,
            LocalDateTime dataFim) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (clienteId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (formaPagamento != null) {
                predicate = cb.and(predicate, cb.equal(root.get("formaPagamento"), formaPagamento));
            }
            if (dataInicio != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdAt"), dataInicio));
            }
            if (dataFim != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("createdAt"), dataFim));
            }

            return predicate;
        };
    }
}
