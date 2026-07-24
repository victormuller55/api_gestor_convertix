package br.net.convertix.gestor.util;

import br.net.convertix.gestor.dto.response.AssinaturaResponse;
import br.net.convertix.gestor.dto.response.HistoricoStatusPagamentoResponse;
import br.net.convertix.gestor.dto.response.PagamentoResponse;
import br.net.convertix.gestor.dto.response.PagamentoResumoResponse;
import br.net.convertix.gestor.entity.Assinatura;
import br.net.convertix.gestor.entity.HistoricoStatusPagamento;
import br.net.convertix.gestor.entity.Pagamento;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class FinanceiroMapperUtil {

    public PagamentoResponse toResponse(Pagamento pagamento) {
        return toResponse(pagamento, false);
    }

    public PagamentoResponse toResponse(Pagamento pagamento, boolean incluirHistorico) {
        if (pagamento == null) {
            return null;
        }

        return PagamentoResponse.builder()
                .id(pagamento.getId())
                .clienteId(pagamento.getCliente() != null ? pagamento.getCliente().getId() : null)
                .clienteNomeEmpresa(pagamento.getCliente() != null ? pagamento.getCliente().getNomeEmpresa() : null)
                .siteId(pagamento.getSite() != null ? pagamento.getSite().getId() : null)
                .assinaturaId(pagamento.getAssinatura() != null ? pagamento.getAssinatura().getId() : null)
                .asaasPaymentId(pagamento.getAsaasPaymentId())
                .valor(pagamento.getValor())
                .descricao(pagamento.getDescricao())
                .status(pagamento.getStatus())
                .formaPagamento(pagamento.getFormaPagamento())
                .parcelas(pagamento.getParcelas())
                .qrCode(pagamento.getQrCode())
                .codigoPix(pagamento.getCodigoPix())
                .invoiceUrl(pagamento.getInvoiceUrl())
                .comprovanteUrl(pagamento.getComprovanteUrl())
                .dataVencimento(pagamento.getDataVencimento())
                .dataConfirmacao(pagamento.getDataConfirmacao())
                .mensagemAsaas(pagamento.getMensagemAsaas())
                .externalReference(pagamento.getExternalReference())
                .createdAt(pagamento.getCreatedAt())
                .updatedAt(pagamento.getUpdatedAt())
                .historicoStatus(incluirHistorico
                        ? toHistoricoList(pagamento.getHistoricoStatus())
                        : null)
                .build();
    }

    public PagamentoResumoResponse toResumo(Pagamento pagamento) {
        if (pagamento == null) {
            return null;
        }
        return PagamentoResumoResponse.builder()
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
                .build();
    }

    public AssinaturaResponse toResponse(Assinatura assinatura) {
        return toResponse(assinatura, Collections.emptyList());
    }

    public AssinaturaResponse toResponse(Assinatura assinatura, List<Pagamento> cobrancas) {
        if (assinatura == null) {
            return null;
        }
        return AssinaturaResponse.builder()
                .id(assinatura.getId())
                .clienteId(assinatura.getCliente() != null ? assinatura.getCliente().getId() : null)
                .clienteNomeEmpresa(assinatura.getCliente() != null ? assinatura.getCliente().getNomeEmpresa() : null)
                .siteId(assinatura.getSite() != null ? assinatura.getSite().getId() : null)
                .siteNome(assinatura.getSite() != null ? assinatura.getSite().getNome() : null)
                .siteTipo(assinatura.getSite() != null ? assinatura.getSite().getTipo() : null)
                .asaasSubscriptionId(assinatura.getAsaasSubscriptionId())
                .valor(assinatura.getValor())
                .descricao(assinatura.getDescricao())
                .ciclo(assinatura.getCiclo())
                .formaPagamento(assinatura.getFormaPagamento())
                .status(assinatura.getStatus())
                .proximaCobranca(assinatura.getProximaCobranca())
                .mensagemAsaas(assinatura.getMensagemAsaas())
                .externalReference(assinatura.getExternalReference())
                .createdAt(assinatura.getCreatedAt())
                .updatedAt(assinatura.getUpdatedAt())
                .cobrancas(cobrancas == null ? Collections.emptyList()
                        : cobrancas.stream().map(FinanceiroMapperUtil::toResponse).collect(Collectors.toList()))
                .build();
    }

    public HistoricoStatusPagamentoResponse toHistorico(HistoricoStatusPagamento historico) {
        if (historico == null) {
            return null;
        }
        return HistoricoStatusPagamentoResponse.builder()
                .id(historico.getId())
                .statusAnterior(historico.getStatusAnterior())
                .statusNovo(historico.getStatusNovo())
                .origem(historico.getOrigem())
                .mensagem(historico.getMensagem())
                .createdAt(historico.getCreatedAt())
                .build();
    }

    private List<HistoricoStatusPagamentoResponse> toHistoricoList(List<HistoricoStatusPagamento> historicos) {
        if (historicos == null || historicos.isEmpty()) {
            return Collections.emptyList();
        }
        return historicos.stream()
                .map(FinanceiroMapperUtil::toHistorico)
                .collect(Collectors.toList());
    }
}
