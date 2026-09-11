package br.net.convertix.gestor.repository.spec;

import br.net.convertix.gestor.entity.Pagamento;
import br.net.convertix.gestor.enums.FormaPagamento;
import br.net.convertix.gestor.enums.StatusPagamento;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.Collection;

@UtilityClass
public class PagamentoSpecification {

    public Specification<Pagamento> comFiltros(
            Long clienteId,
            Collection<StatusPagamento> statuses,
            FormaPagamento formaPagamento,
            LocalDate dataInicio,
            LocalDate dataFim) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (clienteId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (!CollectionUtils.isEmpty(statuses)) {
                predicate = cb.and(predicate, root.get("status").in(statuses));
            }
            if (formaPagamento != null) {
                predicate = cb.and(predicate, cb.equal(root.get("formaPagamento"), formaPagamento));
            }
            if (dataInicio != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("dataVencimento"), dataInicio));
            }
            if (dataFim != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("dataVencimento"), dataFim));
            }

            return predicate;
        };
    }
}
