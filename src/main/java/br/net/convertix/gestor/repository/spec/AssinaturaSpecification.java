package br.net.convertix.gestor.repository.spec;

import br.net.convertix.gestor.entity.Assinatura;
import br.net.convertix.gestor.enums.StatusAssinatura;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class AssinaturaSpecification {

    public Specification<Assinatura> comFiltros(Long clienteId, StatusAssinatura status) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (clienteId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }

            return predicate;
        };
    }
}
