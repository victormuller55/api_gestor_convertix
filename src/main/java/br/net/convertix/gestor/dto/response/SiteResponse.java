package br.net.convertix.gestor.dto.response;

import br.net.convertix.gestor.enums.SituacaoAssinaturaSite;
import br.net.convertix.gestor.enums.StatusSite;
import br.net.convertix.gestor.enums.TipoSite;
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
public class SiteResponse {

    private Long id;
    private Long clienteId;
    private String clienteNomeEmpresa;
    private String nome;
    private TipoSite tipo;
    private String dominio;
    private String subdominio;
    private StatusSite status;
    private SituacaoAssinaturaSite situacaoAssinatura;
    private SiteDominioResponse dominioInfo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
