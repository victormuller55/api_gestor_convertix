package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.response.DashboardInicioResponse;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.Alerta;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.AssinaturaDestaque;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.AtividadeRecente;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.ClienteReceitaTop;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.ContagemChave;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.ContagemValor;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.Distribuicoes;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.Financeiro;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.Funil;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.FunilTaxas;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.Kpis;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.PagamentoDashboardItem;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.PontoQuantidadeMensal;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.PontoReceitaMensal;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.ProdutoBloco;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.ProdutoRecente;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.Produtos;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.Series;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.SiteRecenteTop;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.Tops;
import br.net.convertix.gestor.dto.response.DashboardInicioResponse.UsuarioResumo;
import br.net.convertix.gestor.entity.Assinatura;
import br.net.convertix.gestor.entity.Cliente;
import br.net.convertix.gestor.entity.Pagamento;
import br.net.convertix.gestor.entity.Site;
import br.net.convertix.gestor.entity.Usuario;
import br.net.convertix.gestor.enums.CicloAssinatura;
import br.net.convertix.gestor.enums.FormaPagamento;
import br.net.convertix.gestor.enums.StatusAplicativoMobile;
import br.net.convertix.gestor.enums.StatusAssinatura;
import br.net.convertix.gestor.enums.StatusPagamento;
import br.net.convertix.gestor.enums.StatusSite;
import br.net.convertix.gestor.enums.TipoProdutoDashboard;
import br.net.convertix.gestor.enums.TipoSite;
import br.net.convertix.gestor.enums.TipoUsuario;
import br.net.convertix.gestor.exception.ResourceNotFoundException;
import br.net.convertix.gestor.repository.AplicativoMobileRepository;
import br.net.convertix.gestor.repository.AssinaturaRepository;
import br.net.convertix.gestor.repository.BioLinkRepository;
import br.net.convertix.gestor.repository.ClienteRepository;
import br.net.convertix.gestor.repository.PagamentoRepository;
import br.net.convertix.gestor.repository.SiteRepository;
import br.net.convertix.gestor.repository.UsuarioRepository;
import br.net.convertix.gestor.security.SecurityUtil;
import br.net.convertix.gestor.security.UsuarioAutenticado;
import br.net.convertix.gestor.util.FinanceiroMapperUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardInicioService {

    private static final List<StatusPagamento> STATUS_PAGOS = List.of(
            StatusPagamento.RECEIVED, StatusPagamento.CONFIRMED);
    private static final List<StatusPagamento> STATUS_PENDENTES = List.of(
            StatusPagamento.PENDING, StatusPagamento.OVERDUE);
    private static final List<StatusPagamento> STATUS_PAGAMENTO_DASHBOARD = List.of(
            StatusPagamento.PENDING,
            StatusPagamento.RECEIVED,
            StatusPagamento.CONFIRMED,
            StatusPagamento.OVERDUE,
            StatusPagamento.CANCELLED,
            StatusPagamento.REFUNDED,
            StatusPagamento.FAILED);

    private static final int DIAS_ASSINATURA_VENCENDO = 7;

    private final AutorizacaoService autorizacaoService;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final SiteRepository siteRepository;
    private final BioLinkRepository bioLinkRepository;
    private final AplicativoMobileRepository aplicativoMobileRepository;
    private final PagamentoRepository pagamentoRepository;
    private final AssinaturaRepository assinaturaRepository;

    @Transactional(readOnly = true)
    public DashboardInicioResponse obterDashboard(
            int meses,
            String tipoProdutoParam,
            int limiteAtividades,
            int limiteAlertas,
            int limiteTops) {
        int periodoMeses = clamp(meses, 3, 24, 12);
        int limAtividades = clamp(limiteAtividades, 1, 30, 10);
        int limAlertas = clamp(limiteAlertas, 1, 30, 10);
        int limTops = clamp(limiteTops, 1, 10, 5);
        TipoProdutoDashboard tipoProduto = TipoProdutoDashboard.fromParam(tipoProdutoParam);

        UsuarioAutenticado auth = SecurityUtil.getUsuarioLogado();
        Long clienteId = autorizacaoService.getClienteIdFiltro();
        String escopo = auth.isAdmin() ? TipoUsuario.ADMIN.name() : TipoUsuario.CLIENTE.name();

        LocalDateTime inicioSeries = YearMonth.now().minusMonths(periodoMeses - 1L).atDay(1).atStartOfDay();
        YearMonth mesAtual = YearMonth.now();
        YearMonth mesAnterior = mesAtual.minusMonths(1);
        LocalDateTime inicioMesAtual = mesAtual.atDay(1).atStartOfDay();
        LocalDateTime inicioProximoMes = mesAtual.plusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime inicioMesAnterior = mesAnterior.atDay(1).atStartOfDay();

        List<Pagamento> pagamentos = pagamentoRepository.findAllComProduto(clienteId);
        List<Assinatura> assinaturasAtivas = assinaturaRepository.findPorStatusComProduto(
                clienteId, StatusAssinatura.ACTIVE);

        Kpis kpis = montarKpis(clienteId, inicioMesAtual, inicioProximoMes, inicioMesAnterior);
        Financeiro financeiro = montarFinanceiro(
                tipoProduto,
                pagamentos,
                assinaturasAtivas,
                inicioSeries,
                inicioMesAtual,
                inicioProximoMes,
                inicioMesAnterior,
                Math.max(limTops, 10));
        Produtos produtos = montarProdutos(clienteId, assinaturasAtivas, inicioSeries, limTops);
        Distribuicoes distribuicoes = montarDistribuicoes(clienteId);
        Series series = montarSeries(clienteId, inicioSeries);
        Funil funil = montarFunil(clienteId);
        List<Alerta> alertas = montarAlertas(clienteId, limAlertas);
        Tops tops = montarTops(clienteId, limTops);
        AssinaturaDestaque assinaturaDestaque = montarAssinaturaDestaque(clienteId);
        List<PagamentoDashboardItem> ultimosPagamentos = financeiro.getUltimosPagamentos();
        List<AtividadeRecente> atividades = montarAtividades(clienteId, limAtividades);

        return DashboardInicioResponse.builder()
                .geradoEm(Instant.now())
                .escopo(escopo)
                .periodoMeses(periodoMeses)
                .tipoProduto(tipoProduto.name())
                .tipoProdutoLabel(tipoProduto.label())
                .usuario(montarUsuario(auth))
                .kpis(kpis)
                .financeiro(financeiro)
                .produtos(produtos)
                .distribuicoes(distribuicoes)
                .series(series)
                .funil(funil)
                .alertas(alertas)
                .tops(tops)
                .assinaturaDestaque(assinaturaDestaque)
                .ultimosPagamentos(ultimosPagamentos)
                .atividadesRecentes(atividades)
                .build();
    }

    private UsuarioResumo montarUsuario(UsuarioAutenticado auth) {
        Usuario usuario = usuarioRepository.findById(auth.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + auth.getId()));

        String nomeEmpresa = clienteRepository.findByUsuarioId(usuario.getId())
                .map(Cliente::getNomeEmpresa)
                .orElse(null);

        return UsuarioResumo.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .tipo(usuario.getTipo())
                .nomeEmpresa(nomeEmpresa)
                .foto(usuario.getFoto())
                .build();
    }

    private Kpis montarKpis(Long clienteId, LocalDateTime inicioMesAtual, LocalDateTime inicioProximoMes,
                            LocalDateTime inicioMesAnterior) {
        long totalSites = siteRepository.contarPorCliente(clienteId);
        long sitesAtivos = siteRepository.contarPorClienteEStatus(clienteId, StatusSite.ATIVO);
        long sitesInativos = siteRepository.contarPorClienteEStatus(clienteId, StatusSite.INATIVO);
        long sitesEmDesenvolvimento = siteRepository.contarPorClienteEStatus(clienteId, StatusSite.EM_DESENVOLVIMENTO);
        long totalBiolinks = bioLinkRepository.contarPorCliente(clienteId);
        long sitesBiolink = siteRepository.contarPorClienteETipo(clienteId, TipoSite.BIOLINK);
        long totalClientes = clienteRepository.contarPorEscopo(clienteId);
        long totalUsuarios = usuarioRepository.contarPorEscopo(clienteId);
        long usuariosAtivos = usuarioRepository.contarAtivosPorEscopo(clienteId);

        long assinaturasAtivas = assinaturaRepository.contarPorClienteEStatus(clienteId, StatusAssinatura.ACTIVE);
        long assinaturasInativas = assinaturaRepository.contarPorClienteEStatus(clienteId, StatusAssinatura.INACTIVE);
        long assinaturasExpiradas = assinaturaRepository.contarPorClienteEStatus(clienteId, StatusAssinatura.EXPIRED);

        BigDecimal totalPago = zero(pagamentoRepository.somarPorStatus(clienteId, STATUS_PAGOS));
        BigDecimal totalPendente = zero(pagamentoRepository.somarPorStatus(clienteId, STATUS_PENDENTES));
        long quantidadePagamentos = pagamentoRepository.contarPorCliente(clienteId);
        long quantidadePendentes = pagamentoRepository.contarPorClienteEStatus(clienteId, StatusPagamento.PENDING);
        long quantidadeVencidos = pagamentoRepository.contarPorClienteEStatus(clienteId, StatusPagamento.OVERDUE);
        long quantidadePagos = pagamentoRepository.contarPorClienteEStatuses(clienteId, STATUS_PAGOS);

        BigDecimal ticketMedio = quantidadePagos == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : totalPago.divide(BigDecimal.valueOf(quantidadePagos), 2, RoundingMode.HALF_UP);

        BigDecimal mrr = calcularMrr(clienteId);
        BigDecimal receitaMesAtual = zero(pagamentoRepository.somarPagoNoPeriodo(
                clienteId, STATUS_PAGOS, inicioMesAtual, inicioProximoMes));
        BigDecimal receitaMesAnterior = zero(pagamentoRepository.somarPagoNoPeriodo(
                clienteId, STATUS_PAGOS, inicioMesAnterior, inicioMesAtual));
        BigDecimal variacao = calcularVariacaoPercentual(receitaMesAtual, receitaMesAnterior);

        return Kpis.builder()
                .totalSites(totalSites)
                .sitesAtivos(sitesAtivos)
                .sitesInativos(sitesInativos)
                .sitesEmDesenvolvimento(sitesEmDesenvolvimento)
                .totalBiolinks(totalBiolinks)
                .sitesBiolink(sitesBiolink)
                .totalClientes(totalClientes)
                .totalUsuarios(totalUsuarios)
                .usuariosAtivos(usuariosAtivos)
                .assinaturasAtivas(assinaturasAtivas)
                .assinaturasInativas(assinaturasInativas)
                .assinaturasExpiradas(assinaturasExpiradas)
                .totalPago(totalPago)
                .totalPendente(totalPendente)
                .quantidadePagamentos(quantidadePagamentos)
                .quantidadePendentes(quantidadePendentes)
                .quantidadeVencidos(quantidadeVencidos)
                .ticketMedioPago(ticketMedio)
                .mrrEstimado(mrr)
                .receitaMesAtual(receitaMesAtual)
                .receitaMesAnterior(receitaMesAnterior)
                .variacaoReceitaPercentual(variacao)
                .build();
    }

    private BigDecimal calcularMrr(Long clienteId) {
        List<Object[]> rows = assinaturaRepository.somarValorPorCiclo(clienteId, StatusAssinatura.ACTIVE);
        BigDecimal mrr = BigDecimal.ZERO;
        for (Object[] row : rows) {
            CicloAssinatura ciclo = (CicloAssinatura) row[0];
            BigDecimal soma = toBigDecimal(row[1]);
            mrr = mrr.add(normalizarParaMensal(soma, ciclo));
        }
        return mrr.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizarParaMensal(BigDecimal valor, CicloAssinatura ciclo) {
        if (valor == null || ciclo == null) {
            return BigDecimal.ZERO;
        }
        return switch (ciclo) {
            case WEEKLY -> valor.multiply(BigDecimal.valueOf(52))
                    .divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
            case BIWEEKLY -> valor.multiply(BigDecimal.valueOf(26))
                    .divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
            case MONTHLY -> valor;
            case BIMONTHLY -> valor.divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
            case QUARTERLY -> valor.divide(BigDecimal.valueOf(3), 8, RoundingMode.HALF_UP);
            case SEMIANNUALLY -> valor.divide(BigDecimal.valueOf(6), 8, RoundingMode.HALF_UP);
            case YEARLY -> valor.divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
        };
    }

    private Financeiro montarFinanceiro(
            TipoProdutoDashboard tipoProduto,
            List<Pagamento> pagamentos,
            List<Assinatura> assinaturasAtivas,
            LocalDateTime inicioSeries,
            LocalDateTime inicioMesAtual,
            LocalDateTime inicioProximoMes,
            LocalDateTime inicioMesAnterior,
            int limitePagamentos) {
        List<Pagamento> filtrados = pagamentos.stream()
                .filter(pagamento -> tipoProduto.inclui(FinanceiroMapperUtil.resolverProdutoTipo(pagamento)))
                .toList();
        List<Assinatura> assinaturasFiltradas = assinaturasAtivas.stream()
                .filter(assinatura -> tipoProduto.inclui(FinanceiroMapperUtil.resolverProdutoTipo(assinatura)))
                .toList();

        BigDecimal totalPago = BigDecimal.ZERO;
        BigDecimal totalPendente = BigDecimal.ZERO;
        BigDecimal receitaMesAtual = BigDecimal.ZERO;
        BigDecimal receitaMesAnterior = BigDecimal.ZERO;
        long quantidadePendentes = 0;
        long quantidadeVencidos = 0;
        long quantidadePagos = 0;

        Map<StatusPagamento, Long> statusQtd = new EnumMap<>(StatusPagamento.class);
        Map<StatusPagamento, BigDecimal> statusValor = new EnumMap<>(StatusPagamento.class);
        Map<FormaPagamento, Long> formaQtd = new EnumMap<>(FormaPagamento.class);
        Map<FormaPagamento, BigDecimal> formaValor = new EnumMap<>(FormaPagamento.class);
        Map<String, BigDecimal> pagoPorMes = new HashMap<>();
        Map<String, BigDecimal> pendentePorMes = new HashMap<>();
        Map<String, Long> qtdPagoPorMes = new HashMap<>();
        Map<String, Long> qtdPendentePorMes = new HashMap<>();

        for (Pagamento pagamento : filtrados) {
            BigDecimal valor = zero(pagamento.getValor());
            StatusPagamento status = pagamento.getStatus();
            if (status != null) {
                statusQtd.merge(status, 1L, Long::sum);
                statusValor.merge(status, valor, BigDecimal::add);
            }
            if (pagamento.getFormaPagamento() != null) {
                formaQtd.merge(pagamento.getFormaPagamento(), 1L, Long::sum);
                formaValor.merge(pagamento.getFormaPagamento(), valor, BigDecimal::add);
            }
            if (STATUS_PAGOS.contains(status)) {
                totalPago = totalPago.add(valor);
                quantidadePagos++;
                LocalDateTime dataRef = pagamento.getDataConfirmacao() != null
                        ? pagamento.getDataConfirmacao()
                        : pagamento.getCreatedAt();
                if (dataRef != null) {
                    if (!dataRef.isBefore(inicioMesAtual) && dataRef.isBefore(inicioProximoMes)) {
                        receitaMesAtual = receitaMesAtual.add(valor);
                    } else if (!dataRef.isBefore(inicioMesAnterior) && dataRef.isBefore(inicioMesAtual)) {
                        receitaMesAnterior = receitaMesAnterior.add(valor);
                    }
                }
            }
            if (STATUS_PENDENTES.contains(status)) {
                totalPendente = totalPendente.add(valor);
            }
            if (status == StatusPagamento.PENDING) {
                quantidadePendentes++;
            }
            if (status == StatusPagamento.OVERDUE) {
                quantidadeVencidos++;
            }

            LocalDateTime criadoEm = pagamento.getCreatedAt();
            if (criadoEm != null && !criadoEm.isBefore(inicioSeries)) {
                String chave = chaveMes(criadoEm.getYear(), criadoEm.getMonthValue());
                if (STATUS_PAGOS.contains(status)) {
                    pagoPorMes.merge(chave, valor, BigDecimal::add);
                    qtdPagoPorMes.merge(chave, 1L, Long::sum);
                } else if (STATUS_PENDENTES.contains(status)) {
                    pendentePorMes.merge(chave, valor, BigDecimal::add);
                    qtdPendentePorMes.merge(chave, 1L, Long::sum);
                }
            }
        }

        BigDecimal ticketMedio = quantidadePagos == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : totalPago.divide(BigDecimal.valueOf(quantidadePagos), 2, RoundingMode.HALF_UP);

        BigDecimal mrr = BigDecimal.ZERO;
        for (Assinatura assinatura : assinaturasFiltradas) {
            mrr = mrr.add(normalizarParaMensal(assinatura.getValor(), assinatura.getCiclo()));
        }

        List<PontoReceitaMensal> receitaMensal = new ArrayList<>();
        YearMonth cursor = YearMonth.from(inicioSeries);
        YearMonth fim = YearMonth.now();
        while (!cursor.isAfter(fim)) {
            String chave = chaveMes(cursor.getYear(), cursor.getMonthValue());
            String label = String.format(Locale.ROOT, "%04d-%02d", cursor.getYear(), cursor.getMonthValue());
            receitaMensal.add(PontoReceitaMensal.builder()
                    .ano(cursor.getYear())
                    .mes(cursor.getMonthValue())
                    .label(label)
                    .valorPago(pagoPorMes.getOrDefault(chave, BigDecimal.ZERO))
                    .valorPendente(pendentePorMes.getOrDefault(chave, BigDecimal.ZERO))
                    .quantidadePagos(qtdPagoPorMes.getOrDefault(chave, 0L))
                    .quantidadePendentes(qtdPendentePorMes.getOrDefault(chave, 0L))
                    .build());
            cursor = cursor.plusMonths(1);
        }

        Map<String, BigDecimal> receitaProdutoValor = new LinkedHashMap<>();
        Map<String, Long> receitaProdutoQtd = new LinkedHashMap<>();
        for (TipoProdutoDashboard tipo : List.of(
                TipoProdutoDashboard.BIOLINK,
                TipoProdutoDashboard.LANDING_PAGE,
                TipoProdutoDashboard.SITE_COMERCIAL,
                TipoProdutoDashboard.APLICATIVO_MOBILE)) {
            receitaProdutoValor.put(tipo.name(), BigDecimal.ZERO);
            receitaProdutoQtd.put(tipo.name(), 0L);
        }
        for (Pagamento pagamento : pagamentos) {
            if (!STATUS_PAGOS.contains(pagamento.getStatus())) {
                continue;
            }
            String tipo = FinanceiroMapperUtil.resolverProdutoTipo(pagamento);
            receitaProdutoValor.merge(tipo, zero(pagamento.getValor()), BigDecimal::add);
            receitaProdutoQtd.merge(tipo, 1L, Long::sum);
        }
        List<ContagemValor> receitaPorProduto = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : receitaProdutoValor.entrySet()) {
            if ("OUTROS".equals(entry.getKey())
                    && entry.getValue().compareTo(BigDecimal.ZERO) == 0
                    && receitaProdutoQtd.getOrDefault(entry.getKey(), 0L) == 0) {
                continue;
            }
            receitaPorProduto.add(ContagemValor.builder()
                    .chave(entry.getKey())
                    .label(labelProduto(entry.getKey()))
                    .quantidade(receitaProdutoQtd.getOrDefault(entry.getKey(), 0L))
                    .valor(entry.getValue())
                    .build());
        }

        List<ContagemValor> pagamentosPorStatus = new ArrayList<>();
        for (StatusPagamento status : STATUS_PAGAMENTO_DASHBOARD) {
            pagamentosPorStatus.add(ContagemValor.builder()
                    .chave(status.name())
                    .label(labelStatusPagamento(status))
                    .quantidade(statusQtd.getOrDefault(status, 0L))
                    .valor(statusValor.getOrDefault(status, BigDecimal.ZERO))
                    .build());
        }

        List<ContagemValor> pagamentosPorForma = new ArrayList<>();
        for (FormaPagamento forma : FormaPagamento.values()) {
            pagamentosPorForma.add(ContagemValor.builder()
                    .chave(forma.name())
                    .label(labelFormaPagamento(forma))
                    .quantidade(formaQtd.getOrDefault(forma, 0L))
                    .valor(formaValor.getOrDefault(forma, BigDecimal.ZERO))
                    .build());
        }

        List<PagamentoDashboardItem> ultimosPagamentos = filtrados.stream()
                .sorted(Comparator.comparing(Pagamento::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limitePagamentos)
                .map(this::toPagamentoItem)
                .collect(Collectors.toList());

        return Financeiro.builder()
                .tipoProduto(tipoProduto.name())
                .tipoProdutoLabel(tipoProduto.label())
                .receitaMesAtual(receitaMesAtual)
                .receitaMesAnterior(receitaMesAnterior)
                .variacaoReceitaPercentual(calcularVariacaoPercentual(receitaMesAtual, receitaMesAnterior))
                .totalPago(totalPago)
                .totalPendente(totalPendente)
                .quantidadePagamentos(filtrados.size())
                .quantidadePendentes(quantidadePendentes)
                .quantidadeVencidos(quantidadeVencidos)
                .ticketMedioPago(ticketMedio)
                .mrrEstimado(mrr.setScale(2, RoundingMode.HALF_UP))
                .assinaturasAtivas(assinaturasFiltradas.size())
                .receitaMensal(receitaMensal)
                .receitaPorProduto(receitaPorProduto)
                .pagamentosPorStatus(pagamentosPorStatus)
                .pagamentosPorForma(pagamentosPorForma)
                .ultimosPagamentos(ultimosPagamentos)
                .build();
    }

    private Produtos montarProdutos(
            Long clienteId,
            List<Assinatura> assinaturasAtivas,
            LocalDateTime inicioSeries,
            int limiteRecentes) {
        Map<String, Long> assinaturasPorProduto = new HashMap<>();
        Map<String, BigDecimal> mrrPorProduto = new HashMap<>();
        for (Assinatura assinatura : assinaturasAtivas) {
            String tipo = FinanceiroMapperUtil.resolverProdutoTipo(assinatura);
            assinaturasPorProduto.merge(tipo, 1L, Long::sum);
            mrrPorProduto.merge(tipo, normalizarParaMensal(assinatura.getValor(), assinatura.getCiclo()), BigDecimal::add);
        }

        return Produtos.builder()
                .aplicativos(montarBlocoAplicativos(
                        clienteId, inicioSeries, limiteRecentes, assinaturasPorProduto, mrrPorProduto))
                .biolinks(montarBlocoSite(
                        TipoSite.BIOLINK,
                        "BioLink",
                        clienteId,
                        inicioSeries,
                        limiteRecentes,
                        assinaturasPorProduto,
                        mrrPorProduto))
                .landingPages(montarBlocoSite(
                        TipoSite.LANDING_PAGE,
                        "Landing page",
                        clienteId,
                        inicioSeries,
                        limiteRecentes,
                        assinaturasPorProduto,
                        mrrPorProduto))
                .sitesInstitucionais(montarBlocoSite(
                        TipoSite.SITE_COMERCIAL,
                        "Site institucional",
                        clienteId,
                        inicioSeries,
                        limiteRecentes,
                        assinaturasPorProduto,
                        mrrPorProduto))
                .build();
    }

    private ProdutoBloco montarBlocoSite(
            TipoSite tipo,
            String label,
            Long clienteId,
            LocalDateTime inicioSeries,
            int limiteRecentes,
            Map<String, Long> assinaturasPorProduto,
            Map<String, BigDecimal> mrrPorProduto) {
        Map<StatusSite, Long> porStatusMap = toEnumCountMap(
                siteRepository.contarAgrupadoPorStatusETipo(clienteId, tipo), StatusSite.class);
        long total = siteRepository.contarPorClienteETipo(clienteId, tipo);

        List<ContagemChave> porStatus = new ArrayList<>();
        for (StatusSite status : StatusSite.values()) {
            porStatus.add(ContagemChave.builder()
                    .chave(status.name())
                    .label(labelStatusSite(status))
                    .quantidade(porStatusMap.getOrDefault(status, 0L))
                    .build());
        }

        List<ProdutoRecente> recentes = siteRepository
                .findRecentesPorTipo(clienteId, tipo, PageRequest.of(0, limiteRecentes))
                .stream()
                .map(site -> {
                    Cliente cliente = site.getCliente();
                    return ProdutoRecente.builder()
                            .id(site.getId())
                            .nome(site.getNome())
                            .status(site.getStatus() != null ? site.getStatus().name() : null)
                            .statusLabel(site.getStatus() != null ? labelStatusSite(site.getStatus()) : null)
                            .clienteId(cliente != null ? cliente.getId() : null)
                            .clienteNome(cliente != null ? cliente.getNomeEmpresa() : null)
                            .createdAt(site.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return ProdutoBloco.builder()
                .chave(tipo.name())
                .label(label)
                .total(total)
                .destaque(porStatusMap.getOrDefault(StatusSite.ATIVO, 0L))
                .destaqueLabel("Ativos")
                .assinaturasAtivas(assinaturasPorProduto.getOrDefault(tipo.name(), 0L))
                .mrrEstimado(zero(mrrPorProduto.get(tipo.name())).setScale(2, RoundingMode.HALF_UP))
                .porStatus(porStatus)
                .novosMensal(preencherSerieQuantidade(
                        toMesQuantidadeMap(siteRepository.contarNovosPorMesETipo(clienteId, tipo, inicioSeries)),
                        inicioSeries))
                .recentes(recentes)
                .build();
    }

    private ProdutoBloco montarBlocoAplicativos(
            Long clienteId,
            LocalDateTime inicioSeries,
            int limiteRecentes,
            Map<String, Long> assinaturasPorProduto,
            Map<String, BigDecimal> mrrPorProduto) {
        Map<StatusAplicativoMobile, Long> porStatusMap = toEnumCountMap(
                aplicativoMobileRepository.contarAgrupadoPorStatus(clienteId), StatusAplicativoMobile.class);
        long total = aplicativoMobileRepository.contarPorCliente(clienteId);

        List<ContagemChave> porStatus = new ArrayList<>();
        for (StatusAplicativoMobile status : StatusAplicativoMobile.values()) {
            porStatus.add(ContagemChave.builder()
                    .chave(status.name())
                    .label(labelStatusAplicativo(status))
                    .quantidade(porStatusMap.getOrDefault(status, 0L))
                    .build());
        }

        List<ProdutoRecente> recentes = aplicativoMobileRepository
                .findRecentes(clienteId, PageRequest.of(0, limiteRecentes))
                .stream()
                .map(app -> {
                    Cliente cliente = app.getCliente();
                    return ProdutoRecente.builder()
                            .id(app.getId())
                            .nome(app.getNome())
                            .status(app.getStatus() != null ? app.getStatus().name() : null)
                            .statusLabel(app.getStatus() != null ? labelStatusAplicativo(app.getStatus()) : null)
                            .clienteId(cliente != null ? cliente.getId() : null)
                            .clienteNome(cliente != null ? cliente.getNomeEmpresa() : null)
                            .createdAt(app.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return ProdutoBloco.builder()
                .chave(TipoProdutoDashboard.APLICATIVO_MOBILE.name())
                .label(TipoProdutoDashboard.APLICATIVO_MOBILE.label())
                .total(total)
                .destaque(porStatusMap.getOrDefault(StatusAplicativoMobile.PRODUCAO, 0L))
                .destaqueLabel("Em produção")
                .assinaturasAtivas(assinaturasPorProduto.getOrDefault(
                        TipoProdutoDashboard.APLICATIVO_MOBILE.name(), 0L))
                .mrrEstimado(zero(mrrPorProduto.get(TipoProdutoDashboard.APLICATIVO_MOBILE.name()))
                        .setScale(2, RoundingMode.HALF_UP))
                .porStatus(porStatus)
                .novosMensal(preencherSerieQuantidade(
                        toMesQuantidadeMap(aplicativoMobileRepository.contarNovosPorMes(clienteId, inicioSeries)),
                        inicioSeries))
                .recentes(recentes)
                .build();
    }

    private List<PontoQuantidadeMensal> preencherSerieQuantidade(
            Map<String, Long> quantidadePorMes,
            LocalDateTime inicioSeries) {
        List<PontoQuantidadeMensal> serie = new ArrayList<>();
        YearMonth cursor = YearMonth.from(inicioSeries);
        YearMonth fim = YearMonth.now();
        while (!cursor.isAfter(fim)) {
            String chave = chaveMes(cursor.getYear(), cursor.getMonthValue());
            String label = String.format(Locale.ROOT, "%04d-%02d", cursor.getYear(), cursor.getMonthValue());
            serie.add(pontoQuantidade(
                    cursor.getYear(),
                    cursor.getMonthValue(),
                    label,
                    quantidadePorMes.getOrDefault(chave, 0L)));
            cursor = cursor.plusMonths(1);
        }
        return serie;
    }

    private Distribuicoes montarDistribuicoes(Long clienteId) {
        Map<StatusSite, Long> sitesStatus = toEnumCountMap(
                siteRepository.contarAgrupadoPorStatus(clienteId), StatusSite.class);
        Map<TipoSite, Long> sitesTipo = toEnumCountMap(
                siteRepository.contarAgrupadoPorTipo(clienteId), TipoSite.class);
        Map<StatusAssinatura, Long> assinStatus = toEnumCountMap(
                assinaturaRepository.contarAgrupadoPorStatus(clienteId), StatusAssinatura.class);
        Map<CicloAssinatura, Long> assinCiclo = toEnumCountMap(
                assinaturaRepository.contarAgrupadoPorCiclo(clienteId), CicloAssinatura.class);

        Map<StatusPagamento, BigDecimal> pagStatusValor = new EnumMap<>(StatusPagamento.class);
        Map<StatusPagamento, Long> pagStatusQtd = new EnumMap<>(StatusPagamento.class);
        for (Object[] row : pagamentoRepository.agregarPorStatus(clienteId)) {
            StatusPagamento status = (StatusPagamento) row[0];
            pagStatusQtd.put(status, ((Number) row[1]).longValue());
            pagStatusValor.put(status, toBigDecimal(row[2]));
        }

        Map<FormaPagamento, Long> pagFormaQtd = new EnumMap<>(FormaPagamento.class);
        Map<FormaPagamento, BigDecimal> pagFormaValor = new EnumMap<>(FormaPagamento.class);
        for (Object[] row : pagamentoRepository.agregarPorForma(clienteId)) {
            FormaPagamento forma = (FormaPagamento) row[0];
            if (forma == null) {
                continue;
            }
            pagFormaQtd.put(forma, ((Number) row[1]).longValue());
            pagFormaValor.put(forma, toBigDecimal(row[2]));
        }

        List<ContagemChave> sitesPorStatus = new ArrayList<>();
        for (StatusSite status : StatusSite.values()) {
            sitesPorStatus.add(ContagemChave.builder()
                    .chave(status.name())
                    .label(labelStatusSite(status))
                    .quantidade(sitesStatus.getOrDefault(status, 0L))
                    .build());
        }

        List<ContagemChave> sitesPorTipo = new ArrayList<>();
        for (TipoSite tipo : TipoSite.values()) {
            sitesPorTipo.add(ContagemChave.builder()
                    .chave(tipo.name())
                    .label(labelTipoSite(tipo))
                    .quantidade(sitesTipo.getOrDefault(tipo, 0L))
                    .build());
        }

        List<ContagemValor> pagamentosPorStatus = new ArrayList<>();
        for (StatusPagamento status : STATUS_PAGAMENTO_DASHBOARD) {
            pagamentosPorStatus.add(ContagemValor.builder()
                    .chave(status.name())
                    .label(labelStatusPagamento(status))
                    .quantidade(pagStatusQtd.getOrDefault(status, 0L))
                    .valor(pagStatusValor.getOrDefault(status, BigDecimal.ZERO))
                    .build());
        }

        List<ContagemValor> pagamentosPorForma = new ArrayList<>();
        for (FormaPagamento forma : FormaPagamento.values()) {
            pagamentosPorForma.add(ContagemValor.builder()
                    .chave(forma.name())
                    .label(labelFormaPagamento(forma))
                    .quantidade(pagFormaQtd.getOrDefault(forma, 0L))
                    .valor(pagFormaValor.getOrDefault(forma, BigDecimal.ZERO))
                    .build());
        }

        List<ContagemChave> assinaturasPorStatus = new ArrayList<>();
        for (StatusAssinatura status : StatusAssinatura.values()) {
            assinaturasPorStatus.add(ContagemChave.builder()
                    .chave(status.name())
                    .label(null)
                    .quantidade(assinStatus.getOrDefault(status, 0L))
                    .build());
        }

        List<ContagemChave> assinaturasPorCiclo = new ArrayList<>();
        for (CicloAssinatura ciclo : CicloAssinatura.values()) {
            assinaturasPorCiclo.add(ContagemChave.builder()
                    .chave(ciclo.name())
                    .label(null)
                    .quantidade(assinCiclo.getOrDefault(ciclo, 0L))
                    .build());
        }

        return Distribuicoes.builder()
                .sitesPorStatus(sitesPorStatus)
                .sitesPorTipo(sitesPorTipo)
                .pagamentosPorStatus(pagamentosPorStatus)
                .pagamentosPorForma(pagamentosPorForma)
                .assinaturasPorStatus(assinaturasPorStatus)
                .assinaturasPorCiclo(assinaturasPorCiclo)
                .build();
    }

    private Series montarSeries(Long clienteId, LocalDateTime inicioSeries) {
        Map<String, Object[]> receitaMap = new HashMap<>();
        for (Object[] row : pagamentoRepository.agregarReceitaMensal(
                clienteId, inicioSeries, STATUS_PAGOS, STATUS_PENDENTES)) {
            int ano = ((Number) row[0]).intValue();
            int mes = ((Number) row[1]).intValue();
            receitaMap.put(chaveMes(ano, mes), row);
        }

        Map<String, Long> clientesMap = toMesQuantidadeMap(clienteRepository.contarNovosPorMes(clienteId, inicioSeries));
        Map<String, Long> sitesMap = toMesQuantidadeMap(siteRepository.contarNovosPorMes(clienteId, inicioSeries));
        Map<String, Long> assinaturasMap = toMesQuantidadeMap(assinaturaRepository.contarNovasPorMes(clienteId, inicioSeries));

        List<PontoReceitaMensal> receitaMensal = new ArrayList<>();
        List<PontoQuantidadeMensal> novosClientes = new ArrayList<>();
        List<PontoQuantidadeMensal> novosSites = new ArrayList<>();
        List<PontoQuantidadeMensal> novasAssinaturas = new ArrayList<>();

        YearMonth cursor = YearMonth.from(inicioSeries);
        YearMonth fim = YearMonth.now();
        while (!cursor.isAfter(fim)) {
            int ano = cursor.getYear();
            int mes = cursor.getMonthValue();
            String chave = chaveMes(ano, mes);
            String label = String.format(Locale.ROOT, "%04d-%02d", ano, mes);

            Object[] receita = receitaMap.get(chave);
            receitaMensal.add(PontoReceitaMensal.builder()
                    .ano(ano)
                    .mes(mes)
                    .label(label)
                    .valorPago(receita == null ? BigDecimal.ZERO : toBigDecimal(receita[2]))
                    .valorPendente(receita == null ? BigDecimal.ZERO : toBigDecimal(receita[3]))
                    .quantidadePagos(receita == null ? 0L : ((Number) receita[4]).longValue())
                    .quantidadePendentes(receita == null ? 0L : ((Number) receita[5]).longValue())
                    .build());

            novosClientes.add(pontoQuantidade(ano, mes, label, clientesMap.getOrDefault(chave, 0L)));
            novosSites.add(pontoQuantidade(ano, mes, label, sitesMap.getOrDefault(chave, 0L)));
            novasAssinaturas.add(pontoQuantidade(ano, mes, label, assinaturasMap.getOrDefault(chave, 0L)));

            cursor = cursor.plusMonths(1);
        }

        return Series.builder()
                .receitaMensal(receitaMensal)
                .novosClientesMensal(novosClientes)
                .novosSitesMensal(novosSites)
                .novasAssinaturasMensal(novasAssinaturas)
                .build();
    }

    private Funil montarFunil(Long clienteId) {
        long clientes = clienteRepository.contarPorEscopo(clienteId);
        long comSite = clienteRepository.contarComSite(clienteId);
        long comAssinatura = clienteRepository.contarComAssinatura(clienteId);
        long comPago = clienteRepository.contarComPagamentoPago(clienteId, STATUS_PAGOS);

        return Funil.builder()
                .clientes(clientes)
                .clientesComSite(comSite)
                .clientesComAssinatura(comAssinatura)
                .clientesComPagamentoPago(comPago)
                .taxas(FunilTaxas.builder()
                        .clienteParaSite(percentual(comSite, clientes))
                        .siteParaAssinatura(percentual(comAssinatura, comSite))
                        .assinaturaParaPago(percentual(comPago, comAssinatura))
                        .clienteParaPago(percentual(comPago, clientes))
                        .build())
                .build();
    }

    private List<Alerta> montarAlertas(Long clienteId, int limite) {
        List<Alerta> alertas = new ArrayList<>();
        PageRequest page = PageRequest.of(0, limite);

        for (Pagamento pagamento : pagamentoRepository.findPorStatus(clienteId, StatusPagamento.OVERDUE, page)) {
            Cliente cliente = pagamento.getCliente();
            LocalDateTime dataRef = pagamento.getDataVencimento() != null
                    ? pagamento.getDataVencimento().atStartOfDay()
                    : pagamento.getCreatedAt();
            alertas.add(Alerta.builder()
                    .id("pagamento_overdue_" + pagamento.getId())
                    .tipo("PAGAMENTO_VENCIDO")
                    .severidade("CRITICAL")
                    .titulo("Pagamento vencido")
                    .mensagem("Fatura #" + pagamento.getId() + " está OVERDUE.")
                    .entidade("PAGAMENTO")
                    .entidadeId(pagamento.getId())
                    .clienteId(cliente != null ? cliente.getId() : null)
                    .clienteNome(cliente != null ? cliente.getNomeEmpresa() : null)
                    .dataReferencia(dataRef)
                    .valor(pagamento.getValor())
                    .build());
        }

        LocalDate hoje = LocalDate.now();
        LocalDate fim = hoje.plusDays(DIAS_ASSINATURA_VENCENDO);
        for (Assinatura assinatura : assinaturaRepository.findVencendoEntre(
                clienteId, StatusAssinatura.ACTIVE, hoje, fim, page)) {
            Cliente cliente = assinatura.getCliente();
            long dias = java.time.temporal.ChronoUnit.DAYS.between(hoje, assinatura.getProximaCobranca());
            String nomeCliente = cliente != null ? cliente.getNomeEmpresa() : "Cliente";
            alertas.add(Alerta.builder()
                    .id("assinatura_vencendo_" + assinatura.getId())
                    .tipo("ASSINATURA_VENCENDO")
                    .severidade("WARNING")
                    .titulo("Assinatura próxima da cobrança")
                    .mensagem(nomeCliente + " tem cobrança em " + dias + " dia(s).")
                    .entidade("ASSINATURA")
                    .entidadeId(assinatura.getId())
                    .clienteId(cliente != null ? cliente.getId() : null)
                    .clienteNome(cliente != null ? cliente.getNomeEmpresa() : null)
                    .dataReferencia(assinatura.getProximaCobranca().atStartOfDay())
                    .valor(assinatura.getValor())
                    .build());
        }

        for (Site site : siteRepository.findPorStatus(clienteId, StatusSite.INATIVO, page)) {
            Cliente cliente = site.getCliente();
            alertas.add(Alerta.builder()
                    .id("site_inativo_" + site.getId())
                    .tipo("SITE_INATIVO")
                    .severidade("INFO")
                    .titulo("Site inativo")
                    .mensagem("O site " + site.getNome() + " está INATIVO.")
                    .entidade("SITE")
                    .entidadeId(site.getId())
                    .clienteId(cliente != null ? cliente.getId() : null)
                    .clienteNome(cliente != null ? cliente.getNomeEmpresa() : null)
                    .dataReferencia(null)
                    .valor(null)
                    .build());
        }

        Map<String, Integer> ordemSeveridade = Map.of(
                "CRITICAL", 0,
                "WARNING", 1,
                "INFO", 2);

        return alertas.stream()
                .sorted(Comparator
                        .comparingInt((Alerta a) -> ordemSeveridade.getOrDefault(a.getSeveridade(), 99))
                        .thenComparing(a -> a.getDataReferencia() != null ? a.getDataReferencia() : LocalDateTime.MAX))
                .limit(limite)
                .collect(Collectors.toList());
    }

    private Tops montarTops(Long clienteId, int limite) {
        PageRequest page = PageRequest.of(0, limite);

        List<ClienteReceitaTop> clientesPorReceita = pagamentoRepository
                .topClientesPorReceita(clienteId, STATUS_PAGOS, page)
                .stream()
                .map(row -> ClienteReceitaTop.builder()
                        .clienteId(((Number) row[0]).longValue())
                        .clienteNome((String) row[1])
                        .totalPago(toBigDecimal(row[2]))
                        .quantidadePagamentos(((Number) row[3]).longValue())
                        .build())
                .collect(Collectors.toList());

        List<SiteRecenteTop> sitesRecentes = siteRepository.findRecentes(clienteId, page).stream()
                .map(site -> SiteRecenteTop.builder()
                        .id(site.getId())
                        .nome(site.getNome())
                        .tipo(site.getTipo())
                        .status(site.getStatus())
                        .clienteId(site.getCliente() != null ? site.getCliente().getId() : null)
                        .clienteNome(site.getCliente() != null ? site.getCliente().getNomeEmpresa() : null)
                        .createdAt(site.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return Tops.builder()
                .clientesPorReceita(clientesPorReceita)
                .sitesRecentes(sitesRecentes)
                .build();
    }

    private AssinaturaDestaque montarAssinaturaDestaque(Long clienteId) {
        Assinatura assinatura = assinaturaRepository
                .findPorStatus(clienteId, StatusAssinatura.ACTIVE, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(null);

        if (assinatura == null) {
            return AssinaturaDestaque.builder().ativa(false).build();
        }

        Cliente cliente = assinatura.getCliente();
        return AssinaturaDestaque.builder()
                .ativa(true)
                .assinaturaId(assinatura.getId())
                .descricao(assinatura.getDescricao())
                .status(assinatura.getStatus())
                .valor(assinatura.getValor())
                .ciclo(assinatura.getCiclo())
                .metodoPagamento(assinatura.getFormaPagamento())
                .proximaCobranca(assinatura.getProximaCobranca() != null
                        ? assinatura.getProximaCobranca().atStartOfDay()
                        : null)
                .clienteId(cliente != null ? cliente.getId() : null)
                .clienteNome(cliente != null ? cliente.getNomeEmpresa() : null)
                .build();
    }

    private List<AtividadeRecente> montarAtividades(Long clienteId, int limite) {
        PageRequest page = PageRequest.of(0, limite);
        List<AtividadeRecente> atividades = new ArrayList<>();

        for (Pagamento pagamento : pagamentoRepository.findRecentes(clienteId, page)) {
            Cliente cliente = pagamento.getCliente();
            atividades.add(AtividadeRecente.builder()
                    .id("pagamento_" + pagamento.getId() + "_created")
                    .tipo("PAGAMENTO_CRIADO")
                    .titulo("Novo pagamento")
                    .descricao("Fatura de " + formatarMoeda(pagamento.getValor()) + " criada")
                    .entidade("PAGAMENTO")
                    .entidadeId(pagamento.getId())
                    .clienteId(cliente != null ? cliente.getId() : null)
                    .clienteNome(cliente != null ? cliente.getNomeEmpresa() : null)
                    .createdAt(pagamento.getCreatedAt())
                    .build());
        }

        for (Site site : siteRepository.findRecentes(clienteId, page)) {
            Cliente cliente = site.getCliente();
            atividades.add(AtividadeRecente.builder()
                    .id("site_" + site.getId() + "_created")
                    .tipo("SITE_CRIADO")
                    .titulo("Novo site")
                    .descricao("Site " + site.getNome() + " criado")
                    .entidade("SITE")
                    .entidadeId(site.getId())
                    .clienteId(cliente != null ? cliente.getId() : null)
                    .clienteNome(cliente != null ? cliente.getNomeEmpresa() : null)
                    .createdAt(site.getCreatedAt())
                    .build());
        }

        for (Assinatura assinatura : assinaturaRepository.findRecentes(clienteId, page)) {
            Cliente cliente = assinatura.getCliente();
            atividades.add(AtividadeRecente.builder()
                    .id("assinatura_" + assinatura.getId() + "_created")
                    .tipo("ASSINATURA_CRIADA")
                    .titulo("Nova assinatura")
                    .descricao((assinatura.getDescricao() != null ? assinatura.getDescricao() : "Assinatura")
                            + " de " + formatarMoeda(assinatura.getValor()))
                    .entidade("ASSINATURA")
                    .entidadeId(assinatura.getId())
                    .clienteId(cliente != null ? cliente.getId() : null)
                    .clienteNome(cliente != null ? cliente.getNomeEmpresa() : null)
                    .createdAt(assinatura.getCreatedAt())
                    .build());
        }

        if (clienteId == null) {
            for (Cliente cliente : clienteRepository.findRecentes(null, page)) {
                atividades.add(AtividadeRecente.builder()
                        .id("cliente_" + cliente.getId() + "_created")
                        .tipo("CLIENTE_CRIADO")
                        .titulo("Novo cliente")
                        .descricao("Cliente " + cliente.getNomeEmpresa() + " criado")
                        .entidade("CLIENTE")
                        .entidadeId(cliente.getId())
                        .clienteId(cliente.getId())
                        .clienteNome(cliente.getNomeEmpresa())
                        .createdAt(cliente.getCreatedAt())
                        .build());
            }
        }

        return atividades.stream()
                .sorted(Comparator.comparing(AtividadeRecente::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limite)
                .collect(Collectors.toList());
    }

    private PagamentoDashboardItem toPagamentoItem(Pagamento pagamento) {
        Cliente cliente = pagamento.getCliente();
        String produtoTipo = FinanceiroMapperUtil.resolverProdutoTipo(pagamento);
        return PagamentoDashboardItem.builder()
                .id(pagamento.getId())
                .valor(pagamento.getValor())
                .descricao(pagamento.getDescricao())
                .status(pagamento.getStatus())
                .formaPagamento(pagamento.getFormaPagamento())
                .parcelas(pagamento.getParcelas())
                .asaasPaymentId(pagamento.getAsaasPaymentId())
                .invoiceUrl(pagamento.getInvoiceUrl())
                .comprovanteUrl(pagamento.getComprovanteUrl())
                .createdAt(pagamento.getCreatedAt())
                .dataConfirmacao(pagamento.getDataConfirmacao())
                .clienteId(cliente != null ? cliente.getId() : null)
                .clienteNome(cliente != null ? cliente.getNomeEmpresa() : null)
                .produtoNome(FinanceiroMapperUtil.resolverProdutoNome(pagamento))
                .produtoTipo("OUTROS".equals(produtoTipo) ? null : produtoTipo)
                .produtoTipoLabel(labelProduto(produtoTipo))
                .build();
    }

    private static PontoQuantidadeMensal pontoQuantidade(int ano, int mes, String label, long quantidade) {
        return PontoQuantidadeMensal.builder()
                .ano(ano)
                .mes(mes)
                .label(label)
                .quantidade(quantidade)
                .build();
    }

    private static Map<String, Long> toMesQuantidadeMap(List<Object[]> rows) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            int ano = ((Number) row[0]).intValue();
            int mes = ((Number) row[1]).intValue();
            map.put(chaveMes(ano, mes), ((Number) row[2]).longValue());
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> Map<E, Long> toEnumCountMap(List<Object[]> rows, Class<E> tipo) {
        Map<E, Long> map = new EnumMap<>(tipo);
        for (Object[] row : rows) {
            E chave = (E) row[0];
            map.put(chave, ((Number) row[1]).longValue());
        }
        return map;
    }

    private static String chaveMes(int ano, int mes) {
        return ano + "-" + mes;
    }

    private static BigDecimal zero(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private static BigDecimal toBigDecimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        if (valor instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (valor instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(valor.toString());
    }

    private static BigDecimal calcularVariacaoPercentual(BigDecimal atual, BigDecimal anterior) {
        if (anterior == null || anterior.compareTo(BigDecimal.ZERO) == 0) {
            if (atual != null && atual.compareTo(BigDecimal.ZERO) > 0) {
                return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
            }
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return atual.subtract(anterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(anterior, 2, RoundingMode.HALF_UP);
    }

    private static double percentual(long parte, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(parte)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static String formatarMoeda(BigDecimal valor) {
        BigDecimal v = zero(valor).setScale(2, RoundingMode.HALF_UP);
        return "R$ " + v.toPlainString().replace(".", ",");
    }

    private static String labelStatusSite(StatusSite status) {
        return switch (status) {
            case ATIVO -> "Ativo";
            case INATIVO -> "Inativo";
            case EM_DESENVOLVIMENTO -> "Em desenvolvimento";
        };
    }

    private static String labelTipoSite(TipoSite tipo) {
        return switch (tipo) {
            case BIOLINK -> "BioLink";
            case LANDING_PAGE -> "Landing page";
            case SITE_COMERCIAL -> "Site institucional";
        };
    }

    private static String labelProduto(String chave) {
        if (chave == null || "OUTROS".equals(chave)) {
            return "Outros";
        }
        try {
            return TipoProdutoDashboard.valueOf(chave).label();
        } catch (IllegalArgumentException ignored) {
            return "Outros";
        }
    }

    private static String labelStatusPagamento(StatusPagamento status) {
        return switch (status) {
            case PENDING -> "Pendente";
            case RECEIVED -> "Recebido";
            case CONFIRMED -> "Confirmado";
            case OVERDUE -> "Vencido";
            case REFUNDED -> "Estornado";
            case CANCELLED -> "Cancelado";
            case FAILED -> "Falhou";
            case DELETED -> "Excluído";
        };
    }

    private static String labelFormaPagamento(FormaPagamento forma) {
        return switch (forma) {
            case PIX -> "PIX";
            case CREDIT_CARD -> "Cartão";
            case BOLETO -> "Boleto";
        };
    }

    private static String labelStatusAplicativo(StatusAplicativoMobile status) {
        return switch (status) {
            case DESENVOLVIMENTO -> "Desenvolvimento";
            case HOMOLOGACAO -> "Homologação";
            case PRODUCAO -> "Produção";
            case PAUSADO -> "Pausado";
            case ENCERRADO -> "Encerrado";
        };
    }

    private static int clamp(int valor, int min, int max, int padrao) {
        if (valor < min || valor > max) {
            if (valor == 0) {
                return padrao;
            }
            return Math.max(min, Math.min(max, valor));
        }
        return valor;
    }
}
