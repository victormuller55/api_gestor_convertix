package br.net.convertix.gestor.dto.request;

import br.net.convertix.gestor.enums.BioLinkItemIcone;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class BioLinkItemRequest {

    @NotBlank(message = "O título é obrigatório")
    @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
    private String titulo;

    private Long biolinkId;

    @NotBlank(message = "A URL é obrigatória")
    @Size(max = 2048, message = "A URL deve ter no máximo 2048 caracteres")
    @Pattern(regexp = "https?://.+", message = "A URL deve começar com http:// ou https://")
    private String url;

    private BioLinkItemIcone icone;

    @NotNull(message = "A ordem é obrigatória")
    private Integer ordem;

    @NotNull(message = "O campo ativo é obrigatório")
    private Boolean ativo;
}
