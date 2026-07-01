package br.net.convertix.gestor.repository.spec;

import br.net.convertix.gestor.entity.Site;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class SiteSpecification {

    public Specification<Site> comFiltros(Long id, String query, Long clienteId) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();

            if (id != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("id"), id));
            }

            if (clienteId != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(root.get("cliente").get("id"), clienteId));
            }

            if (query != null && !query.isBlank()) {
                String termo = "%" + query.toLowerCase() + "%";
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nome")), termo),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("dominio")), termo),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("subdominio")), termo)
                ));
            }

            return predicates;
        };
    }
}
