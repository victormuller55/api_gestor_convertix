package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.response.FinanceiroDashboardResponse;
import br.net.convertix.gestor.dto.response.FinanceiroDashboardResponse.AssinaturaAtivaItem;
import br.net.convertix.gestor.dto.response.PagamentoResumoResponse;
import br.net.convertix.gestor.entity.Assinatura;
import br.net.convertix.gestor.entity.Cliente;
import br.net.convertix.gestor.entity.Pagamento;
import br.net.convertix.gestor.enums.SituacaoAssinaturaSite;
import br.net.convertix.gestor.enums.StatusAssinatura;
import br.net.convertix.gestor.enums.StatusPagamento;
import br.net.convertix.gestor.repository.AssinaturaRepository;
import br.net.convertix.gestor.repository.PagamentoRepository;
import br.net.convertix.gestor.util.CicloAssinaturaUtil;
import br.net.convertix.gestor.util.FinanceiroMapperUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinanceiroDashboardService {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final List<StatusPagamento> STATUS_PAGOS = List.of(
            StatusPagamento.RECEIVED, StatusPagamento.CONFIRMED);
    private static final List<StatusPagamento> STATUS_ABERTOS = List.of(
            StatusPagamento.PENDING, StatusPagamento.OVERDUE);

    private final PagamentoRepository pagamentoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final AutorizacaoService autorizacaoService;

    @Transactional(readOnly = true)
    public FinanceiroDashboardResponse obterDashboard(Integer ano, Integer mes) {
        Long clienteId = autorizacaoService.getClienteIdFiltro();
        YearMonth competencia = resolverCompetencia(ano, mes);
        LocalDate inicioMes = competencia.atDay(1);
        LocalDate fimMes = competencia.atEndOfMonth();
        LocalDateTime inicioPeriodo = inicioMes.atStartOfDay();
        LocalDateTime fimPeriodoExclusivo = competencia.plusMonths(1).atDay(1).atStartOfDay();
        LocalDate hoje = LocalDate.now(ZONE);

        BigDecimal totalPago = zero(pagamentoRepository.somarPagoNoPeriodo(
                clienteId, STATUS_PAGOS, inicioPeriodo, fimPeriodoExclusivo));
        long quantidadePagos = pagamentoRepository.contarPagoNoPeriodo(
                clienteId, STATUS_PAGOS, inicioPeriodo, fimPeriodoExclusivo);

        BigDecimal totalPendente = zero(pagamentoRepository.somarPorStatusEVencimento(
                clienteId, STATUS_ABERTOS, inicioMes, fimMes));
        long quantidadePendentes = pagamentoRepository.contarPorStatusEVencimento(
                clienteId, STATUS_ABERTOS, inicioMes, fimMes);

        LocalDate proximaCobranca = pagamentoRepository.findProximoVencimentoAberto(
                clienteId, STATUS_ABERTOS, hoje);
        if (proximaCobranca == null) {
            proximaCobranca = pagamentoRepository.findPrimeiroVencimentoAberto(clienteId, STATUS_ABERTOS);
        }

        Pagamento ultimo = clienteId == null
                ? pagamentoRepository.findTop10ByOrderByCreatedAtDesc().stream().findFirst().orElse(null)
                : pagamentoRepository.findTop10ByClienteIdOrderByCreatedAtDesc(clienteId).stream()
                        .findFirst()
                        .orElse(null);
        PagamentoResumoResponse ultimoResumo = FinanceiroMapperUtil.toResumo(ultimo);

        List<AssinaturaAtivaItem> assinaturasAtivas = montarAssinaturasAtivas(clienteId);
        AssinaturaAtivaItem destaque = assinaturasAtivas.stream().findFirst().orElse(null);
        BigDecimal valorAssinaturas = assinaturasAtivas.stream()
                .map(AssinaturaAtivaItem::getValor)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return FinanceiroDashboardResponse.builder()
                .totalPago(totalPago)
                .totalPendente(totalPendente)
                .quantidadePagamentos(quantidadePagos)
                .quantidadePendentes(quantidadePendentes)
                .ultimoPagamento(ultimoResumo)
                .proximaCobranca(proximaCobranca)
                .assinaturaAtiva(!assinaturasAtivas.isEmpty())
                .valorAssinatura(destaque != null ? valorAssinaturas : null)
                .metodoPagamentoAssinatura(destaque != null ? destaque.getFormaPagamento() : null)
                .descricaoAssinatura(destaque != null ? destaque.getDescricao() : null)
                .statusUltimoPagamento(ultimo != null ? ultimo.getStatus() : null)
                .assinaturasAtivas(assinaturasAtivas)
                .build();
    }

    private List<AssinaturaAtivaItem> montarAssinaturasAtivas(Long clienteId) {
        List<Assinatura> ativas = assinaturaRepository.findPorStatusComProduto(
                clienteId, StatusAssinatura.ACTIVE);
        Map<Long, LocalDate> proximoAbertoPorAssinatura = new HashMap<>();
        if (!ativas.isEmpty()) {
            List<Long> ids = ativas.stream().map(Assinatura::getId).toList();
            for (Object[] row : pagamentoRepository.findPrimeiroVencimentoAbertoPorAssinaturas(ids, STATUS_ABERTOS)) {
                Long assinaturaId = asLong(row[0]);
                LocalDate vencimento = asLocalDate(row[1]);
                if (assinaturaId != null && vencimento != null) {
                    proximoAbertoPorAssinatura.put(assinaturaId, vencimento);
                }
            }
        }
        return ativas.stream()
                .map(assinatura -> toAssinaturaAtivaItem(
                        assinatura,
                        proximoAbertoPorAssinatura.getOrDefault(assinatura.getId(), assinatura.getProximaCobranca())))
                .sorted(Comparator
                        .comparing((AssinaturaAtivaItem item) -> item.getSituacao() == SituacaoAssinaturaSite.VENCIDO
                                ? 0 : 1)
                        .thenComparing(item -> item.getDescricao() == null ? "" : item.getDescricao(),
                                String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private AssinaturaAtivaItem toAssinaturaAtivaItem(Assinatura assinatura, LocalDate proximaCobranca) {
        Cliente cliente = assinatura.getCliente();
        SituacaoAssinaturaSite situacao = resolverSituacao(assinatura);
        return AssinaturaAtivaItem.builder()
                .id(assinatura.getId())
                .descricao(assinatura.getDescricao())
                .valor(assinatura.getValor())
                .ciclo(assinatura.getCiclo())
                .cicloLabel(CicloAssinaturaUtil.label(assinatura.getCiclo()))
                .formaPagamento(assinatura.getFormaPagamento())
                .clienteNome(cliente != null ? cliente.getNomeEmpresa() : null)
                .produtoNome(FinanceiroMapperUtil.resolverProdutoNome(assinatura))
                .proximaCobranca(proximaCobranca)
                .situacao(situacao)
                .situacaoLabel(situacaoLabel(situacao))
                .build();
    }

    private SituacaoAssinaturaSite resolverSituacao(Assinatura assinatura) {
        if (assinatura == null || assinatura.getStatus() == StatusAssinatura.INACTIVE) {
            return SituacaoAssinaturaSite.DESATIVADO;
        }
        if (assinatura.getStatus() == StatusAssinatura.EXPIRED) {
            return SituacaoAssinaturaSite.VENCIDO;
        }
        boolean inadimplente = pagamentoRepository.existsByAssinaturaIdAndStatusIn(
                assinatura.getId(),
                List.of(StatusPagamento.OVERDUE));
        return inadimplente ? SituacaoAssinaturaSite.VENCIDO : SituacaoAssinaturaSite.EM_DIA;
    }

    private YearMonth resolverCompetencia(Integer ano, Integer mes) {
        YearMonth atual = YearMonth.now(ZONE);
        if (ano == null || mes == null || mes < 1 || mes > 12 || ano < 2000 || ano > 2100) {
            return atual;
        }
        return YearMonth.of(ano, mes);
    }

    private String situacaoLabel(SituacaoAssinaturaSite situacao) {
        if (situacao == null) {
            return null;
        }
        return switch (situacao) {
            case EM_DIA -> "Em dia";
            case VENCIDO -> "Vencido";
            case DESATIVADO -> "Desativado";
        };
    }

    private BigDecimal zero(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private LocalDate asLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        return null;
    }
}
