package br.net.convertix.gestor.service;

import br.net.convertix.gestor.entity.Assinatura;
import br.net.convertix.gestor.entity.HistoricoStatusPagamento;
import br.net.convertix.gestor.entity.Pagamento;
import br.net.convertix.gestor.enums.FormaPagamento;
import br.net.convertix.gestor.enums.OrigemAlteracaoStatus;
import br.net.convertix.gestor.enums.StatusAssinatura;
import br.net.convertix.gestor.enums.StatusPagamento;
import br.net.convertix.gestor.integration.payment.PaymentGateway;
import br.net.convertix.gestor.repository.AssinaturaRepository;
import br.net.convertix.gestor.repository.PagamentoRepository;
import br.net.convertix.gestor.util.CicloAssinaturaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * Garante a cobrança local da assinatura com base em {@code proximaCobranca}.
 * Materializa/sincroniza pagamentos gerados pelo gateway da assinatura (Asaas),
 * sem criar cobrança avulsa (evita duplicar cobrança).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CobrancaAssinaturaService {

    private final AssinaturaRepository assinaturaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final PaymentGateway paymentGateway;
    private final LiberacaoService liberacaoService;

    @Transactional
    public void garantirProximoPagamento(Assinatura assinatura) {
        if (assinatura == null || !StringUtils.hasText(assinatura.getAsaasSubscriptionId())) {
            return;
        }

        atualizarAssinaturaDoGateway(assinatura);

        if (assinatura.getStatus() != StatusAssinatura.ACTIVE) {
            log.debug("Assinatura {} não está ACTIVE — não gera próxima cobrança", assinatura.getId());
            return;
        }

        // A Asaas cria a 1ª cobrança de forma assíncrona após a assinatura.
        List<PaymentGateway.GatewayPayment> cobrancasGateway =
                listarCobrancasComRetry(assinatura.getAsaasSubscriptionId());

        for (PaymentGateway.GatewayPayment cobranca : cobrancasGateway) {
            upsertPagamento(cobranca, assinatura);
        }

        if (assinatura.getProximaCobranca() == null && assinatura.getCiclo() != null) {
            assinatura.setProximaCobranca(CicloAssinaturaUtil.calcularProxima(LocalDate.now(), assinatura.getCiclo()));
            assinaturaRepository.save(assinatura);
        }
        final LocalDate proxima = assinatura.getProximaCobranca();
        if (proxima == null) {
            return;
        }

        boolean temAbertaLocal = pagamentoRepository.existsByAssinaturaIdAndStatusIn(
                assinatura.getId(),
                List.of(StatusPagamento.PENDING, StatusPagamento.OVERDUE));
        boolean temNaDataLocal = pagamentoRepository.existsByAssinaturaIdAndDataVencimento(
                assinatura.getId(), proxima);
        boolean temAbertaGateway = cobrancasGateway.stream()
                .anyMatch(c -> c.status() == StatusPagamento.PENDING || c.status() == StatusPagamento.OVERDUE);

        if (temAbertaLocal || temNaDataLocal) {
            log.debug("Assinatura {} já possui cobrança aberta/local para o ciclo", assinatura.getId());
            return;
        }

        // Após pagar, a Asaas pode demorar a criar a próxima — força geração até a próxima data.
        if (!temAbertaGateway) {
            try {
                paymentGateway.gerarCobrancasAssinaturaAte(assinatura.getAsaasSubscriptionId(), proxima);
                cobrancasGateway = listarCobrancasComRetry(assinatura.getAsaasSubscriptionId());
                for (PaymentGateway.GatewayPayment cobranca : cobrancasGateway) {
                    upsertPagamento(cobranca, assinatura);
                }
                atualizarAssinaturaDoGateway(assinatura);
            } catch (Exception ex) {
                log.warn(
                        "Assinatura {}: falha ao solicitar geração da próxima cobrança até {}: {}",
                        assinatura.getId(),
                        proxima,
                        ex.getMessage());
            }
        }

        PaymentGateway.GatewayPayment daProximaData = cobrancasGateway.stream()
                .filter(c -> proxima.equals(c.dueDate()))
                .findFirst()
                .orElse(null);
        if (daProximaData != null) {
            upsertPagamento(daProximaData, assinatura);
            return;
        }

        PaymentGateway.GatewayPayment pendenteOuVencido = cobrancasGateway.stream()
                .filter(c -> c.status() == StatusPagamento.PENDING || c.status() == StatusPagamento.OVERDUE)
                .findFirst()
                .orElse(null);
        if (pendenteOuVencido != null) {
            upsertPagamento(pendenteOuVencido, assinatura);
            return;
        }

        log.info(
                "Assinatura {}: cobrança para proximaCobranca {} ainda não disponível no gateway — aguardando",
                assinatura.getId(),
                proxima);
    }

    private List<PaymentGateway.GatewayPayment> listarCobrancasComRetry(String asaasSubscriptionId) {
        int maxTentativas = 5;
        long delayMs = 800;
        List<PaymentGateway.GatewayPayment> cobrancas = List.of();

        for (int tentativa = 1; tentativa <= maxTentativas; tentativa++) {
            cobrancas = paymentGateway.listarCobrancasAssinatura(asaasSubscriptionId);
            if (cobrancas != null && !cobrancas.isEmpty()) {
                return cobrancas;
            }
            if (tentativa < maxTentativas) {
                log.debug(
                        "Assinatura {}: cobranças ainda vazias no Asaas (tentativa {}/{}), aguardando {}ms",
                        asaasSubscriptionId,
                        tentativa,
                        maxTentativas,
                        delayMs);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return cobrancas != null ? cobrancas : List.of();
    }

    private void atualizarAssinaturaDoGateway(Assinatura assinatura) {
        try {
            PaymentGateway.GatewaySubscription remoto =
                    paymentGateway.consultarAssinatura(assinatura.getAsaasSubscriptionId());
            if (remoto.status() != null) {
                assinatura.setStatus(remoto.status());
            }
            if (remoto.nextDueDate() != null) {
                assinatura.setProximaCobranca(remoto.nextDueDate());
            } else if (assinatura.getProximaCobranca() == null && assinatura.getCiclo() != null) {
                assinatura.setProximaCobranca(LocalDate.now());
            }
            if (remoto.value() != null) {
                assinatura.setValor(remoto.value());
            }
            if (remoto.message() != null) {
                assinatura.setMensagemAsaas(remoto.message());
            }
            assinaturaRepository.save(assinatura);
        } catch (Exception ex) {
            log.warn(
                    "Falha ao consultar assinatura {} no gateway: {}. Mantendo dados locais.",
                    assinatura.getId(),
                    ex.getMessage());
            if (assinatura.getProximaCobranca() == null && assinatura.getCiclo() != null) {
                assinatura.setProximaCobranca(LocalDate.now());
                assinaturaRepository.save(assinatura);
            }
        }
    }

    /**
     * Avança {@code proximaCobranca} localmente quando o pagamento da assinatura é confirmado
     * e o gateway ainda não refletiu a nova data.
     */
    @Transactional
    public void avancarProximaCobrancaSePago(Assinatura assinatura, Pagamento pagamento) {
        if (assinatura == null || pagamento == null) {
            return;
        }
        if (pagamento.getStatus() != StatusPagamento.RECEIVED
                && pagamento.getStatus() != StatusPagamento.CONFIRMED) {
            return;
        }

        LocalDate vencimentoPago = pagamento.getDataVencimento();
        LocalDate proximaAtual = assinatura.getProximaCobranca();

        // Se a próxima ainda aponta para o vencimento que acabou de ser pago, avança pelo ciclo.
        if (vencimentoPago != null && vencimentoPago.equals(proximaAtual) && assinatura.getCiclo() != null) {
            LocalDate novaProxima = CicloAssinaturaUtil.calcularProxima(vencimentoPago, assinatura.getCiclo());
            assinatura.setProximaCobranca(novaProxima);
            assinaturaRepository.save(assinatura);
            log.info(
                    "Assinatura {}: proximaCobranca avançada de {} para {} após pagamento {}",
                    assinatura.getId(),
                    vencimentoPago,
                    novaProxima,
                    pagamento.getId());
        }
    }

    private void upsertPagamento(PaymentGateway.GatewayPayment gatewayPayment, Assinatura assinatura) {
        if (gatewayPayment == null || !StringUtils.hasText(gatewayPayment.id())) {
            return;
        }

        Pagamento pagamento = pagamentoRepository.findByAsaasPaymentId(gatewayPayment.id()).orElse(null);
        if (pagamento == null) {
            pagamento = Pagamento.builder()
                    .cliente(assinatura.getCliente())
                    .assinatura(assinatura)
                    .site(assinatura.getSite())
                    .asaasPaymentId(gatewayPayment.id())
                    .valor(gatewayPayment.value() != null ? gatewayPayment.value() : assinatura.getValor())
                    .descricao(gatewayPayment.description() != null
                            ? gatewayPayment.description()
                            : assinatura.getDescricao())
                    .status(gatewayPayment.status())
                    .formaPagamento(gatewayPayment.billingType() != null
                            ? gatewayPayment.billingType()
                            : assinatura.getFormaPagamento())
                    .parcelas(gatewayPayment.installmentCount() != null ? gatewayPayment.installmentCount() : 1)
                    .invoiceUrl(gatewayPayment.invoiceUrl())
                    .comprovanteUrl(gatewayPayment.transactionReceiptUrl())
                    .dataVencimento(gatewayPayment.dueDate())
                    .dataConfirmacao(gatewayPayment.confirmedDate())
                    .externalReference(gatewayPayment.externalReference() != null
                            ? gatewayPayment.externalReference()
                            : assinatura.getExternalReference())
                    .mensagemAsaas(gatewayPayment.message())
                    .build();

            enriquecerPixSeNecessario(pagamento, gatewayPayment);
            registrarHistorico(
                    pagamento,
                    null,
                    pagamento.getStatus(),
                    OrigemAlteracaoStatus.CRIACAO,
                    "Cobrança da assinatura gerada com base na próxima cobrança");
            pagamento = pagamentoRepository.save(pagamento);
            liberacaoService.processarStatusPagamento(
                    pagamento.getStatus(), pagamento.getSite(), pagamento.getAssinatura());
            log.info(
                    "Pagamento {} gerado para assinatura {} (vencimento {})",
                    pagamento.getId(),
                    assinatura.getId(),
                    pagamento.getDataVencimento());
            return;
        }

        StatusPagamento anterior = pagamento.getStatus();
        StatusPagamento novo = gatewayPayment.status();

        pagamento.setAssinatura(assinatura);
        if (pagamento.getSite() == null) {
            pagamento.setSite(assinatura.getSite());
        }
        pagamento.setInvoiceUrl(gatewayPayment.invoiceUrl());
        pagamento.setComprovanteUrl(gatewayPayment.transactionReceiptUrl());
        pagamento.setDataVencimento(gatewayPayment.dueDate());
        pagamento.setDataConfirmacao(gatewayPayment.confirmedDate());
        pagamento.setMensagemAsaas(
                gatewayPayment.message() != null ? gatewayPayment.message() : gatewayPayment.rawStatus());
        if (gatewayPayment.value() != null) {
            pagamento.setValor(gatewayPayment.value());
        }
        if (gatewayPayment.description() != null) {
            pagamento.setDescricao(gatewayPayment.description());
        }
        if (gatewayPayment.billingType() != null) {
            pagamento.setFormaPagamento(gatewayPayment.billingType());
        }

        enriquecerPixSeNecessario(pagamento, gatewayPayment);

        if (anterior != novo) {
            pagamento.setStatus(novo);
            registrarHistorico(
                    pagamento,
                    anterior,
                    novo,
                    OrigemAlteracaoStatus.SINCRONIZACAO,
                    "Sincronização de cobrança da assinatura");
        }

        pagamentoRepository.save(pagamento);
        if (anterior != novo) {
            liberacaoService.processarStatusPagamento(
                    pagamento.getStatus(), pagamento.getSite(), pagamento.getAssinatura());
        }
    }

    private void enriquecerPixSeNecessario(Pagamento pagamento, PaymentGateway.GatewayPayment gatewayPayment) {
        if (gatewayPayment.billingType() != FormaPagamento.PIX
                && pagamento.getFormaPagamento() != FormaPagamento.PIX) {
            return;
        }
        if (StringUtils.hasText(pagamento.getCodigoPix()) && StringUtils.hasText(pagamento.getQrCode())) {
            return;
        }
        try {
            PaymentGateway.GatewayPixQrCode pix = paymentGateway.consultarPixQrCode(gatewayPayment.id());
            if (pix != null) {
                pagamento.setQrCode(pix.encodedImage());
                pagamento.setCodigoPix(pix.payload());
            }
        } catch (Exception ex) {
            log.debug("PIX QR ainda indisponível para pagamento {}: {}", gatewayPayment.id(), ex.getMessage());
        }
    }

    private void registrarHistorico(
            Pagamento pagamento,
            StatusPagamento anterior,
            StatusPagamento novo,
            OrigemAlteracaoStatus origem,
            String mensagem) {
        HistoricoStatusPagamento historico = HistoricoStatusPagamento.builder()
                .pagamento(pagamento)
                .statusAnterior(anterior)
                .statusNovo(novo)
                .origem(origem)
                .mensagem(mensagem)
                .build();
        pagamento.getHistoricoStatus().add(historico);
    }
}
