package br.net.convertix.gestor.repository.spec;

import br.net.convertix.gestor.entity.LandingPageLead;
import br.net.convertix.gestor.enums.StatusLandingPageLead;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class LandingPageLeadSpecification {

    public Specification<LandingPageLead> comFiltros(
            Long landingPageId,
            Long clienteId,
            StatusLandingPageLead status,
            String query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();

            if (landingPageId != null) {
                predicates = criteriaBuilder.and(
                        predicates, criteriaBuilder.equal(root.get("landingPage").get("id"), landingPageId));
            }

            if (clienteId != null) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.equal(
                                root.get("landingPage").get("site").get("cliente").get("id"), clienteId));
            }

            if (status != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("status"), status));
            }

            if (query != null && !query.isBlank()) {
                String termo = "%" + query.toLowerCase() + "%";
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nome")), termo),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), termo),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("telefone")), termo)));
            }

            return predicates;
        };
    }
}
