package br.net.convertix.gestor.dto.response;

import br.net.convertix.gestor.enums.BioLinkItemIcone;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
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
public class BioLinkItemPublicoResponse {

    private String titulo;
    private String url;
    private BioLinkItemIcone icone;
    private Integer ordem;
}
