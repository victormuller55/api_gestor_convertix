package br.net.convertix.gestor.dto.response;

import br.net.convertix.gestor.enums.OrigemAlteracaoStatus;
import br.net.convertix.gestor.enums.StatusPagamento;
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
public class HistoricoStatusPagamentoResponse {

    private Long id;
    private StatusPagamento statusAnterior;
    private StatusPagamento statusNovo;
    private OrigemAlteracaoStatus origem;
    private String mensagem;
    private LocalDateTime createdAt;
}
