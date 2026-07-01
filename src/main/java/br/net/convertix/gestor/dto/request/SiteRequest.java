package br.net.convertix.gestor.dto.request;

import br.net.convertix.gestor.enums.StatusSite;
import br.net.convertix.gestor.enums.TipoSite;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
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
public class SiteRequest {

    @NotNull(message = "O cliente é obrigatório")
    private Long clienteId;

    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    @NotNull(message = "O tipo é obrigatório")
    private TipoSite tipo;

    private String dominio;

    private String subdominio;

    @NotNull(message = "O status é obrigatório")
    private StatusSite status;

    @Valid
    private SiteDominioRequest dominioInfo;
}
