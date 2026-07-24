package br.net.convertix.gestor.dto.response;

import br.net.convertix.gestor.enums.CicloAssinatura;
import br.net.convertix.gestor.enums.FormaPagamento;
import br.net.convertix.gestor.enums.StatusAssinatura;
import br.net.convertix.gestor.enums.TipoSite;
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
public class AssinaturaResponse {

    private Long id;
    private Long clienteId;
    private String clienteNomeEmpresa;
    private Long siteId;
    private String siteNome;
    private TipoSite siteTipo;
    private String asaasSubscriptionId;
    private BigDecimal valor;
    private String descricao;
    private CicloAssinatura ciclo;
    private FormaPagamento formaPagamento;
    private StatusAssinatura status;
    private LocalDate proximaCobranca;
    private String mensagemAsaas;
    private String externalReference;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PagamentoResponse> cobrancas;
}
