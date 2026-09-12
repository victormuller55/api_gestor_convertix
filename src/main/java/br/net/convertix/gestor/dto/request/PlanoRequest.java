package br.net.convertix.gestor.dto.request;

import br.net.convertix.gestor.enums.CicloAssinatura;
import br.net.convertix.gestor.enums.TipoProjeto;
import br.net.convertix.gestor.enums.VinculoPlano;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanoRequest {

    @Size(max = 50, message = "O código deve ter no máximo 50 caracteres")
    private String codigo;

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    private String nome;

    @NotNull(message = "O tipo é obrigatório")
    private TipoProjeto tipo;

    @NotNull(message = "O vínculo é obrigatório")
    private VinculoPlano vinculo;

    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
    private BigDecimal valor;

    @NotNull(message = "Informe se o valor é livre")
    private Boolean valorLivre;

    @NotNull(message = "O ciclo é obrigatório")
    private CicloAssinatura ciclo;

    @Size(max = 255, message = "A descrição padrão deve ter no máximo 255 caracteres")
    private String descricaoPadrao;

    @NotNull(message = "Informe se o plano está ativo")
    private Boolean ativo;

    private Integer ordem;
}
