package br.net.convertix.gestor.repository.spec;

import br.net.convertix.gestor.entity.Usuario;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class UsuarioSpecification {

    public Specification<Usuario> comFiltros(Long id, String query, Boolean ativo) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();

            if (id != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("id"), id));
            }

            if (query != null && !query.isBlank()) {
                String termo = "%" + query.toLowerCase() + "%";
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nome")), termo),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), termo)
                ));
            }

            if (ativo != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("ativo"), ativo));
            }

            return predicates;
        };
    }
}
