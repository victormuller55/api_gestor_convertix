package br.net.convertix.gestor.dto.request;

import br.net.convertix.gestor.enums.StatusSite;
import br.net.convertix.gestor.enums.TipoSite;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    private String nome;

    @NotNull(message = "O tipo é obrigatório")
    private TipoSite tipo;

    @Size(max = 255, message = "O domínio deve ter no máximo 255 caracteres")
    private String dominio;

    @Size(max = 100, message = "O subdomínio deve ter no máximo 100 caracteres")
    private String subdominio;

    @NotNull(message = "O status é obrigatório")
    private StatusSite status;

    @Valid
    private SiteDominioRequest dominioInfo;
}
