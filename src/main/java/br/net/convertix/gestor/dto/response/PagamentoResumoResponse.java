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
import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagamentoResumoResponse {

    private Long id;
    private BigDecimal valor;
    private String descricao;
    private StatusPagamento status;
    private FormaPagamento formaPagamento;
    private Integer parcelas;
    private String asaasPaymentId;
    private String invoiceUrl;
    private String comprovanteUrl;
    private LocalDateTime createdAt;
    private LocalDateTime dataConfirmacao;
}
