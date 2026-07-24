package br.net.convertix.gestor.integration.asaas.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTOs internos do Asaas (JSON camelCase, independente do snake_case da API pública).
 */
public final class AsaasApiDtos {

    private AsaasApiDtos() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomerRequest {
        private String name;
        private String email;
        private String cpfCnpj;
        private String phone;
        private String mobilePhone;
        private Boolean notificationDisabled;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomerResponse {
        private String id;
        private String name;
        private String email;
        private String cpfCnpj;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreditCard {
        private String holderName;
        private String number;
        private String expiryMonth;
        private String expiryYear;
        private String ccv;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreditCardHolderInfo {
        private String name;
        private String email;
        private String cpfCnpj;
        private String postalCode;
        private String addressNumber;
        private String addressComplement;
        private String phone;
        private String mobilePhone;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentRequest {
        private String customer;
        private String billingType;
        private BigDecimal value;
        private String dueDate;
        private String description;
        private String externalReference;
        private Integer installmentCount;
        private BigDecimal installmentValue;
        private String creditCardToken;
        private CreditCard creditCard;
        private CreditCardHolderInfo creditCardHolderInfo;
        private String remoteIp;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentResponse {
        private String id;
        private String customer;
        private String subscription;
        private String billingType;
        private BigDecimal value;
        private BigDecimal netValue;
        private String description;
        private String status;
        private String dueDate;
        private String paymentDate;
        private String clientPaymentDate;
        private String confirmedDate;
        private String invoiceUrl;
        private String bankSlipUrl;
        private String transactionReceiptUrl;
        private String externalReference;
        private Integer installmentNumber;
        private String creditCardToken;
        private Boolean deleted;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PixQrCodeResponse {
        private String encodedImage;
        private String payload;
        private String expirationDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RefundRequest {
        private BigDecimal value;
        private String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubscriptionRequest {
        private String customer;
        private String billingType;
        private BigDecimal value;
        private String nextDueDate;
        private String cycle;
        private String description;
        private String externalReference;
        private String creditCardToken;
        private CreditCard creditCard;
        private CreditCardHolderInfo creditCardHolderInfo;
        private String remoteIp;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubscriptionUpdateRequest {
        private BigDecimal value;
        private String nextDueDate;
        private String cycle;
        private String description;
        private String billingType;
        private Boolean updatePendingPayments;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubscriptionResponse {
        private String id;
        private String customer;
        private String billingType;
        private BigDecimal value;
        private String nextDueDate;
        private String cycle;
        private String description;
        private String status;
        private String externalReference;
        private String creditCardToken;
        private Boolean deleted;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListResponse<T> {
        private String object;
        private Boolean hasMore;
        private Integer totalCount;
        private Integer limit;
        private Integer offset;
        private List<T> data;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorResponse {
        private List<ErrorItem> errors;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorItem {
        private String code;
        private String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookPayload {
        private String id;
        private String event;
        private String dateCreated;
        private PaymentResponse payment;
        private SubscriptionResponse subscription;
        private Map<String, Object> raw;
    }
}
