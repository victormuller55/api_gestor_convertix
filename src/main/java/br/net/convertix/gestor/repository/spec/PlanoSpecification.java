package br.net.convertix.gestor.repository.spec;

import br.net.convertix.gestor.entity.Plano;
import br.net.convertix.gestor.enums.TipoProjeto;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class PlanoSpecification {

    public Specification<Plano> comFiltros(String query, TipoProjeto tipo, Boolean ativo) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();

            if (tipo != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("tipo"), tipo));
            }

            if (ativo != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("ativo"), ativo));
            }

            if (query != null && !query.isBlank()) {
                String termo = "%" + query.toLowerCase() + "%";
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nome")), termo),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("descricaoPadrao")), termo),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("codigo")), termo)
                ));
            }

            return predicates;
        };
    }
}
