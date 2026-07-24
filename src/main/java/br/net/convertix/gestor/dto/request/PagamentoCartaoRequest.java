package br.net.convertix.gestor.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagamentoCartaoRequest {

    private Long clienteId;

    private Long siteId;

    @NotNull(message = "O valor é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
    private BigDecimal valor;

    @NotBlank(message = "A descrição é obrigatória")
    @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
    private String descricao;

    private LocalDate dataVencimento;

    @Min(value = 1, message = "Parcelas mínimas: 1")
    @Max(value = 21, message = "Parcelas máximas: 21")
    private Integer parcelas;

    @Size(max = 255, message = "O token do cartão deve ter no máximo 255 caracteres")
    private String creditCardToken;

    @Valid
    private CartaoCreditoRequest creditCard;

    @Valid
    private CartaoTitularRequest creditCardHolderInfo;

    private Boolean remoteIp;

    @Size(max = 100, message = "A referência externa deve ter no máximo 100 caracteres")
    private String externalReference;
}
