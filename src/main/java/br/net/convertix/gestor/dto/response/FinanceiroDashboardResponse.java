package br.net.convertix.gestor.dto.response;

import br.net.convertix.gestor.enums.FormaPagamento;
import br.net.convertix.gestor.enums.StatusPagamento;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceiroDashboardResponse {

    private BigDecimal totalPago;
    private BigDecimal totalPendente;
    private Long quantidadePagamentos;
    private Long quantidadePendentes;
    private PagamentoResumoResponse ultimoPagamento;
    private LocalDate proximaCobranca;
    private Boolean assinaturaAtiva;
    private BigDecimal valorAssinatura;
    private FormaPagamento metodoPagamentoAssinatura;
    private String descricaoAssinatura;
    private StatusPagamento statusUltimoPagamento;
}
