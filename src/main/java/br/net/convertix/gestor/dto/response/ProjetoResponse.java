package br.net.convertix.gestor.dto.response;

import br.net.convertix.gestor.enums.EtapaProjeto;
import br.net.convertix.gestor.enums.TipoProjeto;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjetoResponse {

    private Long id;
    private Long clienteId;
    private String clienteNomeEmpresa;
    private String titulo;
    private TipoProjeto tipo;
    private EtapaProjeto etapa;
    private Long siteId;
    private String siteNome;
    private Long aplicativoMobileId;
    private String aplicativoMobileNome;
    private LocalDate prazo;
    private String descricao;
    private String observacaoInterna;
    private List<HistoricoEtapaProjetoResponse> historicoEtapa;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
