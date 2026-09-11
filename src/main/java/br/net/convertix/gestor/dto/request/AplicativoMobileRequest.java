package br.net.convertix.gestor.dto.request;

import br.net.convertix.gestor.enums.StatusAplicativoMobile;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class AplicativoMobileRequest {

    @NotNull(message = "O cliente é obrigatório")
    private Long clienteId;

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    private String nome;

    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
    private String descricao;

    @NotNull(message = "O status é obrigatório")
    private StatusAplicativoMobile status;

    @Size(max = 255, message = "O package Android deve ter no máximo 255 caracteres")
    @Pattern(
            regexp = "^$|[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+",
            message = "Package Android inválido"
    )
    private String packageAndroid;

    @Size(max = 255, message = "O Bundle ID iOS deve ter no máximo 255 caracteres")
    @Pattern(
            regexp = "^$|[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+",
            message = "Bundle ID iOS inválido"
    )
    private String bundleIdIos;

    @Size(max = 50, message = "A versão Android deve ter no máximo 50 caracteres")
    private String versaoAndroid;

    @Size(max = 50, message = "A versão iOS deve ter no máximo 50 caracteres")
    private String versaoIos;

    @Size(max = 500, message = "O link da Google Play deve ter no máximo 500 caracteres")
    @Pattern(regexp = "^$|https?://.+", message = "O link da Google Play deve começar com http:// ou https://")
    private String urlAndroid;

    @Size(max = 500, message = "O link da App Store deve ter no máximo 500 caracteres")
    @Pattern(regexp = "^$|https?://.+", message = "O link da App Store deve começar com http:// ou https://")
    private String urlIos;

    @Size(max = 500, message = "A URL do ícone deve ter no máximo 500 caracteres")
    @Pattern(regexp = "^$|https?://.+|/uploads/.+", message = "URL do ícone inválida")
    private String iconeUrl;
}
