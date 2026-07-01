package br.net.convertix.gestor.dto.request;

import br.net.convertix.gestor.validation.ValidationGroups;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class UsuarioRequest {

    @NotBlank(
            groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
            message = "O nome é obrigatório")
    private String nome;

    @NotBlank(
            groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
            message = "O email é obrigatório")
    @Email(
            groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
            message = "Email inválido")
    private String email;

    @NotBlank(groups = ValidationGroups.OnCreate.class, message = "A senha é obrigatória")
    @Schema(format = "password")
    private String senha;

    @NotNull(
            groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
            message = "O campo ativo é obrigatório")
    private Boolean ativo;
}
