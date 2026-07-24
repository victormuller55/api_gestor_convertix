package br.net.convertix.gestor.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Email;
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
public class CartaoTitularRequest {

    @NotBlank(message = "O nome do titular é obrigatório")
    @Size(max = 100, message = "O nome do titular deve ter no máximo 100 caracteres")
    private String name;

    @NotBlank(message = "O email do titular é obrigatório")
    @Email(message = "Email do titular inválido")
    @Size(max = 255, message = "O email deve ter no máximo 255 caracteres")
    private String email;

    @NotBlank(message = "O CPF/CNPJ do titular é obrigatório")
    @Size(min = 11, max = 18, message = "CPF/CNPJ inválido")
    @Pattern(regexp = "[\\d.\\-/]+", message = "CPF/CNPJ contém caracteres inválidos")
    private String cpfCnpj;

    @NotBlank(message = "O CEP do titular é obrigatório")
    @Size(min = 8, max = 9, message = "CEP inválido")
    @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido")
    private String postalCode;

    @NotBlank(message = "O número do endereço é obrigatório")
    @Size(max = 20, message = "O número do endereço deve ter no máximo 20 caracteres")
    private String addressNumber;

    @Size(max = 100, message = "O complemento deve ter no máximo 100 caracteres")
    private String addressComplement;

    @NotBlank(message = "O telefone do titular é obrigatório")
    @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres")
    private String phone;

    @Size(max = 20, message = "O celular deve ter no máximo 20 caracteres")
    private String mobilePhone;
}
