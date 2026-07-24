package br.net.convertix.gestor.integration.asaas;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "asaas")
public class AsaasProperties {

    /**
     * URL base da API.
     * Sandbox: https://sandbox.asaas.com/api/v3
     * Produção: https://api.asaas.com/v3
     */
    private String baseUrl = "https://sandbox.asaas.com/api/v3";

    private String apiKey = "";

    /**
     * Token configurado no painel do Asaas para validar o header asaas-access-token.
     */
    private String webhookToken = "";

    private int connectTimeoutMs = 5000;

    private int readTimeoutMs = 20000;

    private int maxRetries = 3;

    private long retryDelayMs = 500;
}
