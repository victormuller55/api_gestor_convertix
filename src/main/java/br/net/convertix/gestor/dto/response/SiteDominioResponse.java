package br.net.convertix.gestor.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
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
public class SiteDominioResponse {

    private BigDecimal valorDominio;
    private Integer duracaoDominio;
    private LocalDate dataCompraDominio;
    private LocalDate dataFimDominio;
    private LocalDate dataRenovacao;
}
