package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.response.FinanceiroDashboardResponse;
import br.net.convertix.gestor.dto.response.PagamentoResumoResponse;
import br.net.convertix.gestor.entity.Assinatura;
import br.net.convertix.gestor.entity.Pagamento;
import br.net.convertix.gestor.enums.StatusAssinatura;
import br.net.convertix.gestor.enums.StatusPagamento;
import br.net.convertix.gestor.repository.AssinaturaRepository;
import br.net.convertix.gestor.repository.PagamentoRepository;
import br.net.convertix.gestor.util.FinanceiroMapperUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FinanceiroDashboardService {

    private final PagamentoRepository pagamentoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final AutorizacaoService autorizacaoService;

    @Transactional(readOnly = true)
    public FinanceiroDashboardResponse obterDashboard() {
        Long clienteId = autorizacaoService.getClienteIdFiltro();

        BigDecimal totalPago = pagamentoRepository.somarPorStatus(
                clienteId, List.of(StatusPagamento.RECEIVED, StatusPagamento.CONFIRMED));
        BigDecimal totalPendente = pagamentoRepository.somarPorStatus(
                clienteId, List.of(StatusPagamento.PENDING, StatusPagamento.OVERDUE));

        long quantidade;
        long quantidadePendentes;
        Pagamento ultimo;
        Optional<Assinatura> assinaturaAtiva;

        if (clienteId == null) {
            quantidade = pagamentoRepository.count();
            quantidadePendentes = pagamentoRepository.countByStatus(StatusPagamento.PENDING);
            ultimo = pagamentoRepository.findTop10ByOrderByCreatedAtDesc().stream().findFirst().orElse(null);
            assinaturaAtiva = assinaturaRepository.findFirstByStatusOrderByCreatedAtDesc(StatusAssinatura.ACTIVE);
        } else {
            quantidade = pagamentoRepository.countByClienteId(clienteId);
            quantidadePendentes = pagamentoRepository.countByClienteIdAndStatus(clienteId, StatusPagamento.PENDING);
            ultimo = pagamentoRepository.findTop10ByClienteIdOrderByCreatedAtDesc(clienteId).stream().findFirst().orElse(null);
            assinaturaAtiva = assinaturaRepository.findFirstByClienteIdAndStatusOrderByCreatedAtDesc(clienteId, StatusAssinatura.ACTIVE);
        }

        PagamentoResumoResponse ultimoResumo = FinanceiroMapperUtil.toResumo(ultimo);
        Assinatura assinatura = assinaturaAtiva.orElse(null);

        return FinanceiroDashboardResponse.builder()
                .totalPago(totalPago != null ? totalPago : BigDecimal.ZERO)
                .totalPendente(totalPendente != null ? totalPendente : BigDecimal.ZERO)
                .quantidadePagamentos(quantidade)
                .quantidadePendentes(quantidadePendentes)
                .ultimoPagamento(ultimoResumo)
                .proximaCobranca(assinatura != null ? assinatura.getProximaCobranca() : null)
                .assinaturaAtiva(assinatura != null)
                .valorAssinatura(assinatura != null ? assinatura.getValor() : null)
                .metodoPagamentoAssinatura(assinatura != null ? assinatura.getFormaPagamento() : null)
                .descricaoAssinatura(assinatura != null ? assinatura.getDescricao() : null)
                .statusUltimoPagamento(ultimo != null ? ultimo.getStatus() : null)
                .build();
    }
}
