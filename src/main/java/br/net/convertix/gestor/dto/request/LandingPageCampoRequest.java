package br.net.convertix.gestor.dto.request;

import br.net.convertix.gestor.enums.TipoLandingPageCampo;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandingPageCampoRequest {

    @NotBlank(message = "O nome interno é obrigatório")
    private String nomeInterno;

    @NotBlank(message = "O label é obrigatório")
    private String label;

    @NotNull(message = "O tipo é obrigatório")
    private TipoLandingPageCampo tipo;

    private String placeholder;

    @NotNull(message = "O campo obrigatório é obrigatório")
    private Boolean obrigatorio;

    @NotNull(message = "A ordem é obrigatória")
    private Integer ordem;

    @NotNull(message = "O campo ativo é obrigatório")
    private Boolean ativo;
}
