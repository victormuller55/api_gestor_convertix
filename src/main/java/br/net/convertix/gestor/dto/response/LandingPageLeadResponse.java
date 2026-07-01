package br.net.convertix.gestor.dto.response;

import br.net.convertix.gestor.enums.StatusLandingPageLead;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandingPageLeadResponse {

    private Long id;
    private Long landingPageId;
    private Long formularioId;
    private String nome;
    private String email;
    private String telefone;
    private String ip;
    private String origem;
    private String userAgent;
    private StatusLandingPageLead status;
    private String observacao;
    private Map<String, String> respostas;
    private LocalDateTime createdAt;
}
