package br.net.convertix.gestor.repository.spec;

import br.net.convertix.gestor.entity.Cliente;
import br.net.convertix.gestor.util.DocumentoUtil;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class ClienteSpecification {

    public Specification<Cliente> comFiltros(Long id, String query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();

            if (id != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("id"), id));
            }

            if (query != null && !query.isBlank()) {
                String termo = "%" + query.toLowerCase() + "%";
                String documentoTermo = "%" + DocumentoUtil.normalizar(query) + "%";

                predicates = criteriaBuilder.and(predicates, criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nomeEmpresa")), termo),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), termo),
                        criteriaBuilder.like(root.get("telefone"), "%" + query + "%"),
                        criteriaBuilder.like(root.get("documento"), documentoTermo)
                ));
            }

            return predicates;
        };
    }
}
