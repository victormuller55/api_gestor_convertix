package br.net.convertix.gestor.dto.response;

import br.net.convertix.gestor.enums.StatusAplicativoMobile;
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
public class AplicativoMobileResponse {

    private Long id;
    private Long clienteId;
    private String clienteNomeEmpresa;
    private String nome;
    private String descricao;
    private StatusAplicativoMobile status;
    private String packageAndroid;
    private String bundleIdIos;
    private String versaoAndroid;
    private String versaoIos;
    private String urlAndroid;
    private String urlIos;
    private String iconeUrl;
    private String documentoRequisitosUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
