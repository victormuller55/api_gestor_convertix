package br.net.convertix.gestor.dto.response;

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
public class BioLinkResponse {

    private Long id;
    private Long siteId;
    private String siteNome;
    private String nomeUsuario;
    private String descricao;
    private String fotoPerfil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
