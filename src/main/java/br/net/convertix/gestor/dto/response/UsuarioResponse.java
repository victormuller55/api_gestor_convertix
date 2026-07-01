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
public class UsuarioResponse {

    private Long id;
    private String nome;
    private String email;
    private Boolean ativo;
    private TipoUsuario tipo;
    private String foto;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
