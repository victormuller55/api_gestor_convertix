package br.net.convertix.gestor.dto.request;

import br.net.convertix.gestor.validation.ValidationGroups;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class ClienteRequest {

    @NotBlank(
            groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
            message = "O nome da empresa é obrigatório")
    @Size(
            max = 200,
            groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
            message = "O nome da empresa deve ter no máximo 200 caracteres")
    private String nomeEmpresa;

    @NotBlank(
            groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
            message = "O documento é obrigatório")
    @Size(
            min = 11,
            max = 18,
            groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
            message = "Documento deve ter entre 11 e 18 caracteres")
    @Pattern(
            regexp = "[\\d.\\-/]+",
            groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
            message = "Documento contém caracteres inválidos")
    private String documento;

    @NotBlank(
            groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
            message = "O email é obrigatório")
    @Email(
            groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
            message = "Email inválido")
    @Size(
            max = 255,
            groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
            message = "O email deve ter no máximo 255 caracteres")
    private String email;

    @NotBlank(groups = ValidationGroups.OnCreate.class, message = "A senha é obrigatória")
    @Size(
            min = 8,
            max = 128,
            groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
            message = "A senha deve ter entre 8 e 128 caracteres")
    @Schema(format = "password")
    private String senha;

    @Size(
            max = 20,
            groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
            message = "O telefone deve ter no máximo 20 caracteres")
    @Pattern(
            regexp = "^$|^\\+?[0-9\\s()-]{8,20}$",
            groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
            message = "Telefone inválido")
    private String telefone;
}
