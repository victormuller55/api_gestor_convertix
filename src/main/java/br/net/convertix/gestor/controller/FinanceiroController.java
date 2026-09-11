package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.response.FinanceiroDashboardResponse;
import br.net.convertix.gestor.service.FinanceiroDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/financeiro")
@RequiredArgsConstructor
@Tag(name = "Financeiro")
@SecurityRequirement(name = "bearerAuth")
public class FinanceiroController {

    private final FinanceiroDashboardService financeiroDashboardService;

    @Operation(summary = "Dashboard financeiro resumido, com filtro de competência (ano/mês)")
    @GetMapping("/dashboard")
    public ResponseEntity<FinanceiroDashboardResponse> dashboard(
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes) {
        return ResponseEntity.ok(financeiroDashboardService.obterDashboard(ano, mes));
    }
}
