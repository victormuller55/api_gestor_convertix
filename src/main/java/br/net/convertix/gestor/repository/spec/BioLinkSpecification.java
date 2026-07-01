package br.net.convertix.gestor.repository.spec;

import br.net.convertix.gestor.entity.BioLink;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class BioLinkSpecification {

    public Specification<BioLink> comFiltros(Long id, Long clienteId) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();

            if (id != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("id"), id));
            }

            if (clienteId != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(root.get("site").get("cliente").get("id"), clienteId));
            }

            return predicates;
        };
    }
}
