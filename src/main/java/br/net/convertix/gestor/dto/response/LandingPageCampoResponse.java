package br.net.convertix.gestor.dto.response;

import br.net.convertix.gestor.enums.TipoLandingPageCampo;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandingPageCampoResponse {

    private Long id;
    private Long formularioId;
    private String nomeInterno;
    private String label;
    private TipoLandingPageCampo tipo;
    private String placeholder;
    private Boolean obrigatorio;
    private Integer ordem;
    private Boolean ativo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
