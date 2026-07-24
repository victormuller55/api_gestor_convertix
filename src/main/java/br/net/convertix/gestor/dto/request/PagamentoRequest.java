package br.net.convertix.gestor.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMin;
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

/**
 * Cria cobrança avulsa sem forma fixa (Asaas UNDEFINED).
 * O cliente escolhe PIX, cartão ou boleto na hora de pagar.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagamentoRequest {

    private Long clienteId;

    private Long siteId;

    @NotNull(message = "O valor é obrigatório")
    @DecimalMin(value = "5.00", message = "O valor mínimo para cobrança (cliente escolhe a forma) é R$ 5,00")
    private BigDecimal valor;

    @NotBlank(message = "A descrição é obrigatória")
    @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
    private String descricao;

    private LocalDate dataVencimento;

    @Size(max = 100, message = "A referência externa deve ter no máximo 100 caracteres")
    private String externalReference;
}
