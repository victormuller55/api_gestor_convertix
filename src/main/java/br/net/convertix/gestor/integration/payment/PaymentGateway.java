package br.net.convertix.gestor.integration.payment;

import br.net.convertix.gestor.enums.CicloAssinatura;
import br.net.convertix.gestor.enums.FormaPagamento;
import br.net.convertix.gestor.enums.StatusAssinatura;
import br.net.convertix.gestor.enums.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrato de gateway de pagamento. Permite trocar Asaas por outro provedor
 * sem alterar Controllers ou regras de negócio.
 */
public interface PaymentGateway {

    GatewayCustomer criarOuAtualizarCliente(GatewayCustomerRequest request);

    GatewayPayment criarPagamento(GatewayPaymentRequest request);

    GatewayPayment consultarPagamento(String gatewayPaymentId);

    GatewayPixQrCode consultarPixQrCode(String gatewayPaymentId);

    GatewayPayment cancelarPagamento(String gatewayPaymentId);

    GatewayPayment estornarPagamento(String gatewayPaymentId, BigDecimal valor, String descricao);

    GatewaySubscription criarAssinatura(GatewaySubscriptionRequest request);

    GatewaySubscription consultarAssinatura(String gatewaySubscriptionId);

    GatewaySubscription atualizarAssinatura(String gatewaySubscriptionId, GatewaySubscriptionUpdateRequest request);

    GatewaySubscription cancelarAssinatura(String gatewaySubscriptionId);

    List<GatewayPayment> listarCobrancasAssinatura(String gatewaySubscriptionId);

    /**
     * Solicita ao gateway a geração antecipada de cobranças da assinatura até a data informada
     * (no Asaas: carnê / paymentBook). Útil após um pagamento, quando a próxima cobrança
     * ainda não foi materializada pelo ciclo natural.
     */
    void gerarCobrancasAssinaturaAte(String gatewaySubscriptionId, LocalDate ate);

    record GatewayCustomerRequest(
            String name,
            String email,
            String cpfCnpj,
            String phone,
            String mobilePhone,
            String existingCustomerId
    ) {
    }

    record GatewayCustomer(String id, String name, String email, String cpfCnpj) {
    }

    record GatewayCreditCard(
            String holderName,
            String number,
            String expiryMonth,
            String expiryYear,
            String ccv
    ) {
    }

    record GatewayCreditCardHolder(
            String name,
            String email,
            String cpfCnpj,
            String postalCode,
            String addressNumber,
            String addressComplement,
            String phone,
            String mobilePhone
    ) {
    }

    record GatewayPaymentRequest(
            String customerId,
            BigDecimal value,
            String description,
            FormaPagamento billingType,
            LocalDate dueDate,
            Integer installmentCount,
            String externalReference,
            String creditCardToken,
            GatewayCreditCard creditCard,
            GatewayCreditCardHolder creditCardHolderInfo,
            String remoteIp
    ) {
    }

    record GatewayPayment(
            String id,
            String customerId,
            String subscriptionId,
            BigDecimal value,
            String description,
            StatusPagamento status,
            FormaPagamento billingType,
            Integer installmentCount,
            String invoiceUrl,
            String transactionReceiptUrl,
            LocalDate dueDate,
            LocalDateTime confirmedDate,
            String externalReference,
            String creditCardToken,
            String rawStatus,
            String message
    ) {
    }

    record GatewayPixQrCode(String encodedImage, String payload, String expirationDate) {
    }

    record GatewaySubscriptionRequest(
            String customerId,
            BigDecimal value,
            String description,
            CicloAssinatura cycle,
            FormaPagamento billingType,
            LocalDate nextDueDate,
            String externalReference,
            String creditCardToken,
            GatewayCreditCard creditCard,
            GatewayCreditCardHolder creditCardHolderInfo,
            String remoteIp
    ) {
    }

    record GatewaySubscriptionUpdateRequest(
            BigDecimal value,
            String description,
            CicloAssinatura cycle,
            FormaPagamento billingType,
            LocalDate nextDueDate,
            Boolean updatePendingPayments
    ) {
    }

    record GatewaySubscription(
            String id,
            String customerId,
            BigDecimal value,
            String description,
            CicloAssinatura cycle,
            FormaPagamento billingType,
            StatusAssinatura status,
            LocalDate nextDueDate,
            String externalReference,
            String creditCardToken,
            String rawStatus,
            String message
    ) {
    }
}
