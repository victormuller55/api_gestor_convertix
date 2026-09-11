package br.net.convertix.gestor.repository.spec;

import br.net.convertix.gestor.entity.AplicativoMobile;
import br.net.convertix.gestor.enums.StatusAplicativoMobile;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class AplicativoMobileSpecification {

    public Specification<AplicativoMobile> comFiltros(
            Long id,
            String query,
            Long clienteId,
            StatusAplicativoMobile status) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();

            if (id != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("id"), id));
            }

            if (clienteId != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(root.get("cliente").get("id"), clienteId));
            }

            if (status != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("status"), status));
            }

            if (query != null && !query.isBlank()) {
                String termo = "%" + query.toLowerCase() + "%";
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nome")), termo),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("descricao")), termo),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("packageAndroid")), termo),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("bundleIdIos")), termo)
                ));
            }

            return predicates;
        };
    }
}
