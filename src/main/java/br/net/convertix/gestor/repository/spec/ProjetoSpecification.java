package br.net.convertix.gestor.repository.spec;

import br.net.convertix.gestor.entity.Projeto;
import br.net.convertix.gestor.enums.EtapaProjeto;
import br.net.convertix.gestor.enums.TipoProjeto;
import jakarta.persistence.criteria.JoinType;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class ProjetoSpecification {

    public Specification<Projeto> comFiltros(
            Long id,
            String query,
            Long clienteId,
            EtapaProjeto etapa,
            TipoProjeto tipo) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (criteriaQuery.getResultType() != Long.class && criteriaQuery.getResultType() != long.class) {
                root.fetch("cliente", JoinType.LEFT);
                root.fetch("site", JoinType.LEFT);
                root.fetch("aplicativoMobile", JoinType.LEFT);
                criteriaQuery.distinct(true);
            }

            var predicates = criteriaBuilder.conjunction();

            if (id != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("id"), id));
            }

            if (clienteId != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(root.get("cliente").get("id"), clienteId));
            }

            if (etapa != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("etapa"), etapa));
            }

            if (tipo != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("tipo"), tipo));
            }

            if (query != null && !query.isBlank()) {
                String termo = "%" + query.toLowerCase() + "%";
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("titulo")), termo),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("descricao")), termo),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("cliente").get("nomeEmpresa")), termo)
                ));
            }

            return predicates;
        };
    }
}
