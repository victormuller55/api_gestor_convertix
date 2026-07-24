package br.net.convertix.gestor.service;

import br.net.convertix.gestor.entity.Assinatura;
import br.net.convertix.gestor.entity.WebhookEvento;
import br.net.convertix.gestor.exception.BusinessException;
import br.net.convertix.gestor.integration.asaas.AsaasProperties;
import br.net.convertix.gestor.integration.asaas.AsaasStatusMapper;
import br.net.convertix.gestor.integration.asaas.dto.AsaasApiDtos;
import br.net.convertix.gestor.integration.payment.PaymentGateway;
import br.net.convertix.gestor.repository.WebhookEventoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class WebhookAsaasService {

    private final WebhookEventoRepository webhookEventoRepository;
    private final PagamentoService pagamentoService;
    private final AssinaturaService assinaturaService;
    private final AsaasProperties asaasProperties;
    private final ObjectMapper asaasObjectMapper;

    public WebhookAsaasService(
            WebhookEventoRepository webhookEventoRepository,
            PagamentoService pagamentoService,
            AssinaturaService assinaturaService,
            AsaasProperties asaasProperties,
            @Qualifier("asaasObjectMapper") ObjectMapper asaasObjectMapper) {
        this.webhookEventoRepository = webhookEventoRepository;
        this.pagamentoService = pagamentoService;
        this.assinaturaService = assinaturaService;
        this.asaasProperties = asaasProperties;
        this.asaasObjectMapper = asaasObjectMapper;
    }

    @Transactional
    public void processar(String accessToken, String payloadRaw) {
        validarToken(accessToken);

        if (!StringUtils.hasText(payloadRaw)) {
            throw new BusinessException("Payload do webhook vazio");
        }

        AsaasApiDtos.WebhookPayload payload;
        try {
            payload = asaasObjectMapper.readValue(payloadRaw, AsaasApiDtos.WebhookPayload.class);
        } catch (Exception ex) {
            log.error("Falha ao parsear webhook Asaas: {}", ex.getMessage());
            throw new BusinessException("Payload do webhook inválido");
        }

        String eventId = resolverEventId(payload, payloadRaw);
        if (StringUtils.hasText(eventId) && webhookEventoRepository.existsByEventId(eventId)) {
            log.info("Webhook Asaas {} já processado (idempotência)", eventId);
            return;
        }

        WebhookEvento evento = WebhookEvento.builder()
                .eventId(eventId)
                .event(payload.getEvent() != null ? payload.getEvent() : "UNKNOWN")
                .payload(payloadRaw)
                .processado(false)
                .build();
        evento = webhookEventoRepository.save(evento);

        try {
            processarEvento(payload);
            evento.setProcessado(true);
            webhookEventoRepository.save(evento);
            log.info("Webhook Asaas processado: event={}, eventId={}", payload.getEvent(), eventId);
        } catch (Exception ex) {
            evento.setMensagemErro(ex.getMessage());
            webhookEventoRepository.save(evento);
            log.error("Erro ao processar webhook Asaas {}: {}", eventId, ex.getMessage(), ex);
            throw ex instanceof BusinessException
                    ? (BusinessException) ex
                    : new BusinessException("Erro ao processar webhook: " + ex.getMessage());
        }
    }

    private void processarEvento(AsaasApiDtos.WebhookPayload payload) {
        String event = payload.getEvent() != null ? payload.getEvent().toUpperCase() : "";

        if (event.startsWith("PAYMENT_") && payload.getPayment() != null) {
            PaymentGateway.GatewayPayment gatewayPayment = toGatewayPayment(payload.getPayment());
            Assinatura assinatura = null;
            if (StringUtils.hasText(payload.getPayment().getSubscription())) {
                assinatura = assinaturaService.buscarPorAsaasId(payload.getPayment().getSubscription());
            }
            pagamentoService.aplicarPagamentoDoWebhook(gatewayPayment, assinatura);
            return;
        }

        if (event.startsWith("SUBSCRIPTION_") && payload.getSubscription() != null) {
            PaymentGateway.GatewaySubscription gatewaySubscription = toGatewaySubscription(payload.getSubscription());
            assinaturaService.aplicarAssinaturaDoWebhook(gatewaySubscription);
        }
    }

    private void validarToken(String accessToken) {
        String expected = asaasProperties.getWebhookToken();
        if (!StringUtils.hasText(expected)) {
            log.error("asaas.webhook-token não configurado — webhook rejeitado");
            throw new BusinessException("Webhook indisponível");
        }
        if (!StringUtils.hasText(accessToken) || !expected.equals(accessToken)) {
            throw new BusinessException("Token do webhook inválido");
        }
    }

    private String resolverEventId(AsaasApiDtos.WebhookPayload payload, String payloadRaw) {
        if (StringUtils.hasText(payload.getId())) {
            return payload.getId();
        }
        try {
            JsonNode node = asaasObjectMapper.readTree(payloadRaw);
            if (node.hasNonNull("id")) {
                return node.get("id").asText();
            }
            String event = node.path("event").asText("UNKNOWN");
            String paymentId = node.path("payment").path("id").asText("");
            String subscriptionId = node.path("subscription").path("id").asText("");
            String dateCreated = node.path("dateCreated").asText("");
            return event + ":" + paymentId + ":" + subscriptionId + ":" + dateCreated;
        } catch (Exception ex) {
            return "evt-" + System.currentTimeMillis();
        }
    }

    private PaymentGateway.GatewayPayment toGatewayPayment(AsaasApiDtos.PaymentResponse payment) {
        String rawStatus = Boolean.TRUE.equals(payment.getDeleted()) ? "DELETED" : payment.getStatus();
        return new PaymentGateway.GatewayPayment(
                payment.getId(),
                payment.getCustomer(),
                payment.getSubscription(),
                payment.getValue(),
                payment.getDescription(),
                AsaasStatusMapper.mapPaymentStatus(rawStatus),
                AsaasStatusMapper.fromAsaasBillingType(payment.getBillingType()),
                payment.getInstallmentNumber(),
                payment.getInvoiceUrl(),
                payment.getTransactionReceiptUrl(),
                parseDate(payment.getDueDate()),
                parseDateTime(payment.getConfirmedDate() != null ? payment.getConfirmedDate() : payment.getPaymentDate()),
                payment.getExternalReference(),
                payment.getCreditCardToken(),
                rawStatus,
                null
        );
    }

    private PaymentGateway.GatewaySubscription toGatewaySubscription(AsaasApiDtos.SubscriptionResponse subscription) {
        String rawStatus = Boolean.TRUE.equals(subscription.getDeleted()) ? "INACTIVE" : subscription.getStatus();
        return new PaymentGateway.GatewaySubscription(
                subscription.getId(),
                subscription.getCustomer(),
                subscription.getValue(),
                subscription.getDescription(),
                AsaasStatusMapper.fromAsaasCycle(subscription.getCycle()),
                AsaasStatusMapper.fromAsaasBillingType(subscription.getBillingType()),
                AsaasStatusMapper.mapSubscriptionStatus(rawStatus),
                parseDate(subscription.getNextDueDate()),
                subscription.getExternalReference(),
                subscription.getCreditCardToken(),
                rawStatus,
                null
        );
    }

    private java.time.LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(value.substring(0, Math.min(10, value.length())));
        } catch (Exception ex) {
            return null;
        }
    }

    private java.time.LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            if (value.length() <= 10) {
                return java.time.LocalDate.parse(value.substring(0, 10)).atStartOfDay();
            }
            return java.time.LocalDateTime.parse(value.replace(" ", "T").substring(0, Math.min(19, value.length())));
        } catch (Exception ex) {
            return null;
        }
    }
}
