package br.net.convertix.gestor.util;

import br.net.convertix.gestor.dto.response.AssinaturaResponse;
import br.net.convertix.gestor.dto.response.HistoricoStatusPagamentoResponse;
import br.net.convertix.gestor.dto.response.PagamentoResponse;
import br.net.convertix.gestor.dto.response.PagamentoResumoResponse;
import br.net.convertix.gestor.entity.AplicativoMobile;
import br.net.convertix.gestor.entity.Assinatura;
import br.net.convertix.gestor.entity.HistoricoStatusPagamento;
import br.net.convertix.gestor.entity.Pagamento;
import br.net.convertix.gestor.entity.Site;
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

        Site site = resolverSite(pagamento);
        AplicativoMobile aplicativo = site == null ? resolverAplicativo(pagamento) : null;
        String produtoNome = site != null
                ? site.getNome()
                : (aplicativo != null ? aplicativo.getNome() : null);
        String produtoTipo = site != null && site.getTipo() != null
                ? site.getTipo().name()
                : (aplicativo != null ? "APLICATIVO_MOBILE" : null);

        return PagamentoResponse.builder()
                .id(pagamento.getId())
                .clienteId(pagamento.getCliente() != null ? pagamento.getCliente().getId() : null)
                .clienteNomeEmpresa(pagamento.getCliente() != null ? pagamento.getCliente().getNomeEmpresa() : null)
                .siteId(site != null ? site.getId() : (pagamento.getSite() != null ? pagamento.getSite().getId() : null))
                .siteNome(site != null ? site.getNome() : null)
                .siteTipo(site != null ? site.getTipo() : null)
                .aplicativoMobileId(aplicativo != null ? aplicativo.getId() : null)
                .aplicativoMobileNome(aplicativo != null ? aplicativo.getNome() : null)
                .produtoNome(produtoNome)
                .produtoTipo(produtoTipo)
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

    /**
     * Site direto do pagamento ou, se ausente, o site da assinatura vinculada.
     */
    private Site resolverSite(Pagamento pagamento) {
        if (pagamento.getSite() != null) {
            return pagamento.getSite();
        }
        Assinatura assinatura = pagamento.getAssinatura();
        if (assinatura != null && assinatura.getSite() != null) {
            return assinatura.getSite();
        }
        return null;
    }

    /**
     * Aplicativo da assinatura vinculada ao pagamento, quando a cobrança não é de um site.
     */
    private AplicativoMobile resolverAplicativo(Pagamento pagamento) {
        Assinatura assinatura = pagamento.getAssinatura();
        if (assinatura != null && assinatura.getAplicativoMobile() != null) {
            return assinatura.getAplicativoMobile();
        }
        return null;
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
                .aplicativoMobileId(assinatura.getAplicativoMobile() != null ? assinatura.getAplicativoMobile().getId() : null)
                .aplicativoMobileNome(assinatura.getAplicativoMobile() != null ? assinatura.getAplicativoMobile().getNome() : null)
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
