package br.net.convertix.gestor.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartaoCreditoRequest {

    @NotBlank(message = "O nome do titular do cartão é obrigatório")
    @Size(max = 100, message = "O nome do titular deve ter no máximo 100 caracteres")
    private String holderName;

    @NotBlank(message = "O número do cartão é obrigatório")
    @Pattern(regexp = "\\d{13,19}", message = "Número do cartão inválido")
    private String number;

    @NotBlank(message = "O mês de expiração é obrigatório")
    @Pattern(regexp = "0[1-9]|1[0-2]", message = "Mês de expiração inválido")
    private String expiryMonth;

    @NotBlank(message = "O ano de expiração é obrigatório")
    @Pattern(regexp = "\\d{2}|\\d{4}", message = "Ano de expiração inválido")
    private String expiryYear;

    @NotBlank(message = "O CVV é obrigatório")
    @Pattern(regexp = "\\d{3,4}", message = "CVV inválido")
    private String ccv;
}
