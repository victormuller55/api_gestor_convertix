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
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagamentoResponse {

    private Long id;
    private Long clienteId;
    private String clienteNomeEmpresa;
    private Long siteId;
    private Long assinaturaId;
    private String asaasPaymentId;
    private BigDecimal valor;
    private String descricao;
    private StatusPagamento status;
    private FormaPagamento formaPagamento;
    private Integer parcelas;
    private String qrCode;
    private String codigoPix;
    private String invoiceUrl;
    private String comprovanteUrl;
    private LocalDate dataVencimento;
    private LocalDateTime dataConfirmacao;
    private String mensagemAsaas;
    private String externalReference;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<HistoricoStatusPagamentoResponse> historicoStatus;
}
