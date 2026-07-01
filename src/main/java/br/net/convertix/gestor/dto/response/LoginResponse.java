package br.net.convertix.gestor.dto.response;

import br.net.convertix.gestor.enums.TipoUsuario;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private Long id;
    private String nome;
    private String email;
    private TipoUsuario tipo;
    private Boolean ativo;
    private String foto;
    private Long clienteId;
    private String nomeEmpresa;
    private String documento;
    private String telefone;
    private String token;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
