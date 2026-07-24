package br.net.convertix.gestor.dto.request;

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
public class LandingPageRequest {

    @NotNull(message = "O site é obrigatório")
    private Long siteId;

    @NotBlank(message = "O slug é obrigatório")
    @Size(min = 2, max = 100, message = "O slug deve ter entre 2 e 100 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Slug deve conter apenas letras, números e hífen")
    private String slug;
}
