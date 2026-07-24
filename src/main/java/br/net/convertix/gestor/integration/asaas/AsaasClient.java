package br.net.convertix.gestor.integration.asaas;

import br.net.convertix.gestor.integration.asaas.dto.AsaasApiDtos;
import br.net.convertix.gestor.integration.payment.PaymentGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsaasClient implements PaymentGateway {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RestClient asaasRestClient;
    private final AsaasProperties properties;
    private final ObjectMapper asaasObjectMapper;

    @Override
    public GatewayCustomer criarOuAtualizarCliente(GatewayCustomerRequest request) {
        if (StringUtils.hasText(request.existingCustomerId())) {
            try {
                AsaasApiDtos.CustomerResponse existing = executeWithRetry(
                        "GET /customers/{id}",
                        () -> asaasRestClient.get()
                                .uri("/customers/{id}", request.existingCustomerId())
                                .retrieve()
                                .body(AsaasApiDtos.CustomerResponse.class));
                if (existing != null && existing.getId() != null) {
                    AsaasApiDtos.CustomerRequest updateBody = toCustomerRequest(request);
                    AsaasApiDtos.CustomerResponse updated = executeWithRetry(
                            "PUT /customers/{id}",
                            () -> asaasRestClient.put()
                                    .uri("/customers/{id}", request.existingCustomerId())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(updateBody)
                                    .retrieve()
                                    .body(AsaasApiDtos.CustomerResponse.class));
                    return toGatewayCustomer(updated);
                }
            } catch (AsaasException ex) {
                log.warn(
                        "Customer Asaas {} indisponível, criando novo: {}",
                        request.existingCustomerId(),
                        ex.getMessage());
            }
        }

        AsaasApiDtos.CustomerResponse created = executeWithRetry(
                "POST /customers",
                () -> asaasRestClient.post()
                        .uri("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(toCustomerRequest(request))
                        .retrieve()
                        .body(AsaasApiDtos.CustomerResponse.class));
        return toGatewayCustomer(created);
    }

    @Override
    public GatewayPayment criarPagamento(GatewayPaymentRequest request) {
        AsaasApiDtos.PaymentRequest body = AsaasApiDtos.PaymentRequest.builder()
                .customer(request.customerId())
                .billingType(AsaasStatusMapper.toAsaasBillingType(request.billingType()))
                .value(request.value())
                .dueDate(formatDate(request.dueDate() != null ? request.dueDate() : LocalDate.now()))
                .description(request.description())
                .externalReference(request.externalReference())
                .creditCardToken(request.creditCardToken())
                .creditCard(toCreditCard(request.creditCard()))
                .creditCardHolderInfo(toHolder(request.creditCardHolderInfo()))
                .remoteIp(request.remoteIp())
                .build();

        if (request.installmentCount() != null && request.installmentCount() > 1) {
            body.setInstallmentCount(request.installmentCount());
            body.setInstallmentValue(request.value()
                    .divide(BigDecimal.valueOf(request.installmentCount()), 2, RoundingMode.HALF_UP));
        }

        AsaasApiDtos.PaymentResponse response = executeWithRetry(
                "POST /payments",
                () -> asaasRestClient.post()
                        .uri("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(AsaasApiDtos.PaymentResponse.class));
        return toGatewayPayment(response);
    }

    @Override
    public GatewayPayment consultarPagamento(String gatewayPaymentId) {
        AsaasApiDtos.PaymentResponse response = executeWithRetry(
                "GET /payments/{id}",
                () -> asaasRestClient.get()
                        .uri("/payments/{id}", gatewayPaymentId)
                        .retrieve()
                        .body(AsaasApiDtos.PaymentResponse.class));
        return toGatewayPayment(response);
    }

    @Override
    public GatewayPixQrCode consultarPixQrCode(String gatewayPaymentId) {
        AsaasApiDtos.PixQrCodeResponse response = executeWithRetry(
                "GET /payments/{id}/pixQrCode",
                () -> asaasRestClient.get()
                        .uri("/payments/{id}/pixQrCode", gatewayPaymentId)
                        .retrieve()
                        .body(AsaasApiDtos.PixQrCodeResponse.class));
        if (response == null) {
            return new GatewayPixQrCode(null, null, null);
        }
        return new GatewayPixQrCode(response.getEncodedImage(), response.getPayload(), response.getExpirationDate());
    }

    @Override
    public GatewayPayment cancelarPagamento(String gatewayPaymentId) {
        AsaasApiDtos.PaymentResponse response = executeWithRetry(
                "DELETE /payments/{id}",
                () -> asaasRestClient.delete()
                        .uri("/payments/{id}", gatewayPaymentId)
                        .retrieve()
                        .body(AsaasApiDtos.PaymentResponse.class));
        return toGatewayPayment(response);
    }

    @Override
    public GatewayPayment estornarPagamento(String gatewayPaymentId, BigDecimal valor, String descricao) {
        AsaasApiDtos.RefundRequest body = AsaasApiDtos.RefundRequest.builder()
                .value(valor)
                .description(descricao)
                .build();

        AsaasApiDtos.PaymentResponse response = executeWithRetry(
                "POST /payments/{id}/refund",
                () -> asaasRestClient.post()
                        .uri("/payments/{id}/refund", gatewayPaymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(AsaasApiDtos.PaymentResponse.class));
        return toGatewayPayment(response);
    }

    @Override
    public GatewaySubscription criarAssinatura(GatewaySubscriptionRequest request) {
        AsaasApiDtos.SubscriptionRequest body = AsaasApiDtos.SubscriptionRequest.builder()
                .customer(request.customerId())
                .billingType(AsaasStatusMapper.toAsaasBillingType(request.billingType()))
                .value(request.value())
                .nextDueDate(formatDate(request.nextDueDate()))
                .cycle(AsaasStatusMapper.toAsaasCycle(request.cycle()))
                .description(request.description())
                .externalReference(request.externalReference())
                .creditCardToken(request.creditCardToken())
                .creditCard(toCreditCard(request.creditCard()))
                .creditCardHolderInfo(toHolder(request.creditCardHolderInfo()))
                .remoteIp(request.remoteIp())
                .build();

        AsaasApiDtos.SubscriptionResponse response = executeWithRetry(
                "POST /subscriptions",
                () -> asaasRestClient.post()
                        .uri("/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(AsaasApiDtos.SubscriptionResponse.class));
        return toGatewaySubscription(response);
    }

    @Override
    public GatewaySubscription consultarAssinatura(String gatewaySubscriptionId) {
        AsaasApiDtos.SubscriptionResponse response = executeWithRetry(
                "GET /subscriptions/{id}",
                () -> asaasRestClient.get()
                        .uri("/subscriptions/{id}", gatewaySubscriptionId)
                        .retrieve()
                        .body(AsaasApiDtos.SubscriptionResponse.class));
        return toGatewaySubscription(response);
    }

    @Override
    public GatewaySubscription atualizarAssinatura(String gatewaySubscriptionId, GatewaySubscriptionUpdateRequest request) {
        AsaasApiDtos.SubscriptionUpdateRequest body = AsaasApiDtos.SubscriptionUpdateRequest.builder()
                .value(request.value())
                .description(request.description())
                .cycle(request.cycle() != null ? AsaasStatusMapper.toAsaasCycle(request.cycle()) : null)
                .billingType(request.billingType() != null ? AsaasStatusMapper.toAsaasBillingType(request.billingType()) : null)
                .nextDueDate(request.nextDueDate() != null ? formatDate(request.nextDueDate()) : null)
                .updatePendingPayments(request.updatePendingPayments())
                .build();

        AsaasApiDtos.SubscriptionResponse response = executeWithRetry(
                "PUT /subscriptions/{id}",
                () -> asaasRestClient.put()
                        .uri("/subscriptions/{id}", gatewaySubscriptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(AsaasApiDtos.SubscriptionResponse.class));
        return toGatewaySubscription(response);
    }

    @Override
    public GatewaySubscription cancelarAssinatura(String gatewaySubscriptionId) {
        AsaasApiDtos.SubscriptionResponse response = executeWithRetry(
                "DELETE /subscriptions/{id}",
                () -> asaasRestClient.delete()
                        .uri("/subscriptions/{id}", gatewaySubscriptionId)
                        .retrieve()
                        .body(AsaasApiDtos.SubscriptionResponse.class));
        return toGatewaySubscription(response);
    }

    @Override
    public List<GatewayPayment> listarCobrancasAssinatura(String gatewaySubscriptionId) {
        AsaasApiDtos.ListResponse<AsaasApiDtos.PaymentResponse> response = executeWithRetry(
                "GET /subscriptions/{id}/payments",
                () -> asaasRestClient.get()
                        .uri("/subscriptions/{id}/payments?limit=100", gatewaySubscriptionId)
                        .retrieve()
                        .body(new ParameterizedTypeReference<AsaasApiDtos.ListResponse<AsaasApiDtos.PaymentResponse>>() {
                        }));

        if (response == null || response.getData() == null) {
            return Collections.emptyList();
        }
        return response.getData().stream().map(this::toGatewayPayment).collect(Collectors.toList());
    }

    @Override
    public void gerarCobrancasAssinaturaAte(String gatewaySubscriptionId, LocalDate ate) {
        if (!StringUtils.hasText(gatewaySubscriptionId) || ate == null) {
            return;
        }
        int month = ate.getMonthValue();
        int year = ate.getYear();
        executeWithRetry(
                "GET /subscriptions/{id}/paymentBook",
                () -> {
                    asaasRestClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/subscriptions/{id}/paymentBook")
                                    .queryParam("month", month)
                                    .queryParam("year", year)
                                    .build(gatewaySubscriptionId))
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .toBodilessEntity();
                    return Boolean.TRUE;
                });
        log.info(
                "Solicitada geração de cobranças da assinatura {} até {}/{}",
                gatewaySubscriptionId,
                month,
                year);
    }

    private AsaasApiDtos.CustomerRequest toCustomerRequest(GatewayCustomerRequest request) {
        return AsaasApiDtos.CustomerRequest.builder()
                .name(request.name())
                .email(request.email())
                .cpfCnpj(request.cpfCnpj())
                .phone(request.phone())
                .mobilePhone(request.mobilePhone() != null ? request.mobilePhone() : request.phone())
                .notificationDisabled(false)
                .build();
    }

    private GatewayCustomer toGatewayCustomer(AsaasApiDtos.CustomerResponse response) {
        if (response == null || response.getId() == null) {
            throw new AsaasException("Resposta inválida ao criar/atualizar cliente no Asaas");
        }
        return new GatewayCustomer(response.getId(), response.getName(), response.getEmail(), response.getCpfCnpj());
    }

    private GatewayPayment toGatewayPayment(AsaasApiDtos.PaymentResponse response) {
        if (response == null || response.getId() == null) {
            throw new AsaasException("Resposta inválida de pagamento no Asaas");
        }
        String rawStatus = Boolean.TRUE.equals(response.getDeleted()) ? "DELETED" : response.getStatus();

        return new GatewayPayment(
                response.getId(),
                response.getCustomer(),
                response.getSubscription(),
                response.getValue(),
                response.getDescription(),
                AsaasStatusMapper.mapPaymentStatus(rawStatus),
                AsaasStatusMapper.fromAsaasBillingType(response.getBillingType()),
                response.getInstallmentNumber(),
                response.getInvoiceUrl(),
                response.getTransactionReceiptUrl(),
                parseDate(response.getDueDate()),
                parseDateTime(response.getConfirmedDate() != null ? response.getConfirmedDate() : response.getPaymentDate()),
                response.getExternalReference(),
                response.getCreditCardToken(),
                rawStatus,
                null
        );
    }

    private GatewaySubscription toGatewaySubscription(AsaasApiDtos.SubscriptionResponse response) {
        if (response == null || response.getId() == null) {
            throw new AsaasException("Resposta inválida de assinatura no Asaas");
        }
        String rawStatus = Boolean.TRUE.equals(response.getDeleted()) ? "INACTIVE" : response.getStatus();
        return new GatewaySubscription(
                response.getId(),
                response.getCustomer(),
                response.getValue(),
                response.getDescription(),
                AsaasStatusMapper.fromAsaasCycle(response.getCycle()),
                AsaasStatusMapper.fromAsaasBillingType(response.getBillingType()),
                AsaasStatusMapper.mapSubscriptionStatus(rawStatus),
                parseDate(response.getNextDueDate()),
                response.getExternalReference(),
                response.getCreditCardToken(),
                rawStatus,
                null
        );
    }

    private AsaasApiDtos.CreditCard toCreditCard(GatewayCreditCard card) {
        if (card == null) {
            return null;
        }
        return AsaasApiDtos.CreditCard.builder()
                .holderName(card.holderName())
                .number(card.number())
                .expiryMonth(card.expiryMonth())
                .expiryYear(card.expiryYear())
                .ccv(card.ccv())
                .build();
    }

    private AsaasApiDtos.CreditCardHolderInfo toHolder(GatewayCreditCardHolder holder) {
        if (holder == null) {
            return null;
        }
        return AsaasApiDtos.CreditCardHolderInfo.builder()
                .name(holder.name())
                .email(holder.email())
                .cpfCnpj(holder.cpfCnpj())
                .postalCode(holder.postalCode())
                .addressNumber(holder.addressNumber())
                .addressComplement(holder.addressComplement())
                .phone(holder.phone())
                .mobilePhone(holder.mobilePhone() != null ? holder.mobilePhone() : holder.phone())
                .build();
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : DATE.format(date);
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, Math.min(10, value.length())));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            if (value.length() <= 10) {
                return LocalDate.parse(value.substring(0, 10)).atStartOfDay();
            }
            return LocalDateTime.parse(value.replace(" ", "T").substring(0, Math.min(19, value.length())));
        } catch (Exception ex) {
            try {
                return LocalDate.parse(value.substring(0, 10)).atStartOfDay();
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private <T> T executeWithRetry(String operation, ApiCall<T> call) {
        int attempts = Math.max(1, properties.getMaxRetries());
        RuntimeException lastError = null;

        for (int i = 1; i <= attempts; i++) {
            try {
                return call.execute();
            } catch (RestClientResponseException ex) {
                String message = extrairMensagemErro(ex.getResponseBodyAsString(), ex.getStatusCode().value());
                log.warn("Asaas {} falhou (tentativa {}/{}): HTTP {} - {}", operation, i, attempts, ex.getStatusCode().value(), message);

                if (ex.getStatusCode().is4xxClientError()) {
                    throw new AsaasException(message);
                }
                lastError = new AsaasException(message, ex);
            } catch (Exception ex) {
                log.warn("Asaas {} falhou (tentativa {}/{}): {}", operation, i, attempts, ex.getMessage());
                lastError = new AsaasException("Erro de comunicação com o Asaas: " + ex.getMessage(), ex);
            }

            if (i < attempts) {
                try {
                    Thread.sleep(properties.getRetryDelayMs() * i);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AsaasException("Interrompido ao comunicar com o Asaas");
                }
            }
        }

        throw lastError != null ? lastError : new AsaasException("Falha ao comunicar com o Asaas");
    }

    private String extrairMensagemErro(String body, int status) {
        if (!StringUtils.hasText(body)) {
            return "Erro Asaas HTTP " + status;
        }
        try {
            AsaasApiDtos.ErrorResponse error = asaasObjectMapper.readValue(body, AsaasApiDtos.ErrorResponse.class);
            if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                return error.getErrors().stream()
                        .map(AsaasApiDtos.ErrorItem::getDescription)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.joining("; "));
            }
        } catch (Exception ignored) {
            // fallback abaixo
        }
        return body.length() > 400 ? body.substring(0, 400) : body;
    }

    @FunctionalInterface
    private interface ApiCall<T> {
        T execute();
    }
}
