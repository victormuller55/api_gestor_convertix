package br.net.convertix.gestor.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class SiteDominioRequest {

    @DecimalMin(value = "0.0", inclusive = false, message = "O valor do domínio deve ser maior que zero")
    private BigDecimal valorDominio;

    @NotNull(message = "A data de compra do domínio é obrigatória")
    private LocalDate dataCompraDominio;

    @NotNull(message = "A data de fim do domínio é obrigatória")
    private LocalDate dataFimDominio;

    private LocalDate dataRenovacao;
}
