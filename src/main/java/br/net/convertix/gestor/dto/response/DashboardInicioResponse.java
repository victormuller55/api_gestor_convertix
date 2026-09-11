package br.net.convertix.gestor.dto.response;

import br.net.convertix.gestor.enums.CicloAssinatura;
import br.net.convertix.gestor.enums.FormaPagamento;
import br.net.convertix.gestor.enums.StatusAssinatura;
import br.net.convertix.gestor.enums.StatusPagamento;
import br.net.convertix.gestor.enums.StatusSite;
import br.net.convertix.gestor.enums.TipoSite;
import br.net.convertix.gestor.enums.TipoUsuario;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardInicioResponse {

    private Instant geradoEm;
    private String escopo;
    private Integer periodoMeses;
    private String tipoProduto;
    private String tipoProdutoLabel;

    private UsuarioResumo usuario;
    private Kpis kpis;
    private Financeiro financeiro;
    private Produtos produtos;
    private Distribuicoes distribuicoes;
    private Series series;
    private Funil funil;

    @Builder.Default
    private List<Alerta> alertas = new ArrayList<>();

    private Tops tops;
    private AssinaturaDestaque assinaturaDestaque;

    @Builder.Default
    private List<PagamentoDashboardItem> ultimosPagamentos = new ArrayList<>();

    @Builder.Default
    private List<AtividadeRecente> atividadesRecentes = new ArrayList<>();

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsuarioResumo {
        private Long id;
        private String nome;
        private String email;
        private TipoUsuario tipo;
        private String nomeEmpresa;
        private String foto;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Kpis {
        private long totalSites;
        private long sitesAtivos;
        private long sitesInativos;
        private long sitesEmDesenvolvimento;
        private long totalBiolinks;
        private long sitesBiolink;
        private long totalClientes;
        private long totalUsuarios;
        private long usuariosAtivos;
        private long assinaturasAtivas;
        private long assinaturasInativas;
        private long assinaturasExpiradas;
        private BigDecimal totalPago;
        private BigDecimal totalPendente;
        private long quantidadePagamentos;
        private long quantidadePendentes;
        private long quantidadeVencidos;
        private BigDecimal ticketMedioPago;
        private BigDecimal mrrEstimado;
        private BigDecimal receitaMesAtual;
        private BigDecimal receitaMesAnterior;
        private BigDecimal variacaoReceitaPercentual;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Distribuicoes {
        @Builder.Default
        private List<ContagemChave> sitesPorStatus = new ArrayList<>();
        @Builder.Default
        private List<ContagemChave> sitesPorTipo = new ArrayList<>();
        @Builder.Default
        private List<ContagemValor> pagamentosPorStatus = new ArrayList<>();
        @Builder.Default
        private List<ContagemValor> pagamentosPorForma = new ArrayList<>();
        @Builder.Default
        private List<ContagemChave> assinaturasPorStatus = new ArrayList<>();
        @Builder.Default
        private List<ContagemChave> assinaturasPorCiclo = new ArrayList<>();
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContagemChave {
        private String chave;
        private String label;
        private long quantidade;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContagemValor {
        private String chave;
        private String label;
        private long quantidade;
        private BigDecimal valor;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Financeiro {
        private String tipoProduto;
        private String tipoProdutoLabel;
        private BigDecimal receitaMesAtual;
        private BigDecimal receitaMesAnterior;
        private BigDecimal variacaoReceitaPercentual;
        private BigDecimal totalPago;
        private BigDecimal totalPendente;
        private long quantidadePagamentos;
        private long quantidadePendentes;
        private long quantidadeVencidos;
        private BigDecimal ticketMedioPago;
        private BigDecimal mrrEstimado;
        private long assinaturasAtivas;
        @Builder.Default
        private List<PontoReceitaMensal> receitaMensal = new ArrayList<>();
        @Builder.Default
        private List<ContagemValor> receitaPorProduto = new ArrayList<>();
        @Builder.Default
        private List<ContagemValor> pagamentosPorStatus = new ArrayList<>();
        @Builder.Default
        private List<ContagemValor> pagamentosPorForma = new ArrayList<>();
        @Builder.Default
        private List<PagamentoDashboardItem> ultimosPagamentos = new ArrayList<>();
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Produtos {
        private ProdutoBloco aplicativos;
        private ProdutoBloco biolinks;
        private ProdutoBloco landingPages;
        private ProdutoBloco sitesInstitucionais;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProdutoBloco {
        private String chave;
        private String label;
        private long total;
        private long destaque;
        private String destaqueLabel;
        private long assinaturasAtivas;
        private BigDecimal mrrEstimado;
        @Builder.Default
        private List<ContagemChave> porStatus = new ArrayList<>();
        @Builder.Default
        private List<PontoQuantidadeMensal> novosMensal = new ArrayList<>();
        @Builder.Default
        private List<ProdutoRecente> recentes = new ArrayList<>();
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProdutoRecente {
        private Long id;
        private String nome;
        private String status;
        private String statusLabel;
        private Long clienteId;
        private String clienteNome;
        private LocalDateTime createdAt;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Series {
        @Builder.Default
        private List<PontoReceitaMensal> receitaMensal = new ArrayList<>();
        @Builder.Default
        private List<PontoQuantidadeMensal> novosClientesMensal = new ArrayList<>();
        @Builder.Default
        private List<PontoQuantidadeMensal> novosSitesMensal = new ArrayList<>();
        @Builder.Default
        private List<PontoQuantidadeMensal> novasAssinaturasMensal = new ArrayList<>();
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PontoReceitaMensal {
        private int ano;
        private int mes;
        private String label;
        private BigDecimal valorPago;
        private BigDecimal valorPendente;
        private long quantidadePagos;
        private long quantidadePendentes;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PontoQuantidadeMensal {
        private int ano;
        private int mes;
        private String label;
        private long quantidade;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Funil {
        private long clientes;
        private long clientesComSite;
        private long clientesComAssinatura;
        private long clientesComPagamentoPago;
        private FunilTaxas taxas;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FunilTaxas {
        private double clienteParaSite;
        private double siteParaAssinatura;
        private double assinaturaParaPago;
        private double clienteParaPago;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Alerta {
        private String id;
        private String tipo;
        private String severidade;
        private String titulo;
        private String mensagem;
        private String entidade;
        private Long entidadeId;
        private Long clienteId;
        private String clienteNome;
        private LocalDateTime dataReferencia;
        private BigDecimal valor;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Tops {
        @Builder.Default
        private List<ClienteReceitaTop> clientesPorReceita = new ArrayList<>();
        @Builder.Default
        private List<SiteRecenteTop> sitesRecentes = new ArrayList<>();
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClienteReceitaTop {
        private Long clienteId;
        private String clienteNome;
        private BigDecimal totalPago;
        private long quantidadePagamentos;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SiteRecenteTop {
        private Long id;
        private String nome;
        private TipoSite tipo;
        private StatusSite status;
        private Long clienteId;
        private String clienteNome;
        private LocalDateTime createdAt;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssinaturaDestaque {
        private boolean ativa;
        private Long assinaturaId;
        private String descricao;
        private StatusAssinatura status;
        private BigDecimal valor;
        private CicloAssinatura ciclo;
        private FormaPagamento metodoPagamento;
        private LocalDateTime proximaCobranca;
        private Long clienteId;
        private String clienteNome;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PagamentoDashboardItem {
        private Long id;
        private BigDecimal valor;
        private String descricao;
        private StatusPagamento status;
        private FormaPagamento formaPagamento;
        private Integer parcelas;
        private String asaasPaymentId;
        private String invoiceUrl;
        private String comprovanteUrl;
        private LocalDateTime createdAt;
        private LocalDateTime dataConfirmacao;
        private Long clienteId;
        private String clienteNome;
        private String produtoNome;
        private String produtoTipo;
        private String produtoTipoLabel;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AtividadeRecente {
        private String id;
        private String tipo;
        private String titulo;
        private String descricao;
        private String entidade;
        private Long entidadeId;
        private Long clienteId;
        private String clienteNome;
        private LocalDateTime createdAt;
    }
}
