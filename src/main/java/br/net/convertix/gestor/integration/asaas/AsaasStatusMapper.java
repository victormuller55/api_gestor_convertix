package br.net.convertix.gestor.integration.asaas;

import br.net.convertix.gestor.enums.CicloAssinatura;
import br.net.convertix.gestor.enums.FormaPagamento;
import br.net.convertix.gestor.enums.StatusAssinatura;
import br.net.convertix.gestor.enums.StatusPagamento;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AsaasStatusMapper {

    public StatusPagamento mapPaymentStatus(String asaasStatus) {
        if (asaasStatus == null || asaasStatus.isBlank()) {
            return StatusPagamento.PENDING;
        }

        return switch (asaasStatus.toUpperCase()) {
            case "PENDING", "AWAITING_RISK_ANALYSIS" -> StatusPagamento.PENDING;
            case "RECEIVED", "RECEIVED_IN_CASH" -> StatusPagamento.RECEIVED;
            case "CONFIRMED" -> StatusPagamento.CONFIRMED;
            case "OVERDUE" -> StatusPagamento.OVERDUE;
            case "REFUNDED", "REFUND_REQUESTED", "REFUND_IN_PROGRESS",
                 "CHARGEBACK_REQUESTED", "CHARGEBACK_DISPUTE", "AWAITING_CHARGEBACK_REVERSAL" -> StatusPagamento.REFUNDED;
            case "DELETED" -> StatusPagamento.DELETED;
            case "DUNNING_REQUESTED", "DUNNING_RECEIVED" -> StatusPagamento.OVERDUE;
            default -> {
                if (asaasStatus.toUpperCase().contains("CANCEL")) {
                    yield StatusPagamento.CANCELLED;
                }
                if (asaasStatus.toUpperCase().contains("FAIL")) {
                    yield StatusPagamento.FAILED;
                }
                yield StatusPagamento.PENDING;
            }
        };
    }

    public StatusAssinatura mapSubscriptionStatus(String asaasStatus) {
        if (asaasStatus == null || asaasStatus.isBlank()) {
            return StatusAssinatura.INACTIVE;
        }
        return switch (asaasStatus.toUpperCase()) {
            case "ACTIVE" -> StatusAssinatura.ACTIVE;
            case "EXPIRED" -> StatusAssinatura.EXPIRED;
            default -> StatusAssinatura.INACTIVE;
        };
    }

    public String toAsaasBillingType(FormaPagamento forma) {
        if (forma == null) {
            return "UNDEFINED";
        }
        return forma.name();
    }

    public FormaPagamento fromAsaasBillingType(String billingType) {
        if (billingType == null || billingType.isBlank()) {
            return null;
        }
        return switch (billingType.toUpperCase()) {
            case "CREDIT_CARD", "DEBIT_CARD" -> FormaPagamento.CREDIT_CARD;
            case "BOLETO" -> FormaPagamento.BOLETO;
            case "PIX" -> FormaPagamento.PIX;
            case "UNDEFINED" -> null;
            default -> null;
        };
    }

    public String toAsaasCycle(CicloAssinatura ciclo) {
        return ciclo == null ? "MONTHLY" : ciclo.name();
    }

    public CicloAssinatura fromAsaasCycle(String cycle) {
        if (cycle == null || cycle.isBlank()) {
            return CicloAssinatura.MONTHLY;
        }
        try {
            return CicloAssinatura.valueOf(cycle.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return CicloAssinatura.MONTHLY;
        }
    }
}
