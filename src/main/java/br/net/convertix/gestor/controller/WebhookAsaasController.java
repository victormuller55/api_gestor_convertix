package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.service.WebhookAsaasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
@Tag(name = "Webhook Asaas")
@SecurityRequirements
public class WebhookAsaasController {

    private final WebhookAsaasService webhookAsaasService;

    @Operation(summary = "Receber eventos enviados pelo Asaas")
    @PostMapping("/asaas")
    public ResponseEntity<Map<String, String>> receber(
            @RequestHeader(value = "asaas-access-token", required = false) String accessToken,
            @RequestBody String payload) {
        webhookAsaasService.processar(accessToken, payload);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
