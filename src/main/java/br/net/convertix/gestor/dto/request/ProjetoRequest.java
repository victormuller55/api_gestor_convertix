package br.net.convertix.gestor.dto.request;

import br.net.convertix.gestor.enums.EtapaProjeto;
import br.net.convertix.gestor.enums.TipoProjeto;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjetoRequest {

    @NotNull(message = "O cliente é obrigatório")
    private Long clienteId;

    @NotBlank(message = "O título é obrigatório")
    @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
    private String titulo;

    @NotNull(message = "O tipo é obrigatório")
    private TipoProjeto tipo;

    @NotNull(message = "A etapa é obrigatória")
    private EtapaProjeto etapa;

    private Long siteId;

    private Long aplicativoMobileId;

    private LocalDate prazo;

    @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
    private String descricao;

    @Size(max = 2000, message = "A observação interna deve ter no máximo 2000 caracteres")
    private String observacaoInterna;
}
