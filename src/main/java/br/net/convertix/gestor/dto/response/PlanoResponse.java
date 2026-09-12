package br.net.convertix.gestor.dto.response;

import br.net.convertix.gestor.enums.CicloAssinatura;
import br.net.convertix.gestor.enums.TipoProjeto;
import br.net.convertix.gestor.enums.VinculoPlano;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanoResponse {

    private Long id;
    private String codigo;
    private String nome;
    private TipoProjeto tipo;
    private VinculoPlano vinculo;
    private BigDecimal valor;
    private boolean valorLivre;
    private CicloAssinatura ciclo;
    private String descricaoPadrao;
    private boolean ativo;
    private int ordem;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
