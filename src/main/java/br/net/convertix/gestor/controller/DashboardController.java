package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.response.DashboardInicioResponse;
import br.net.convertix.gestor.service.DashboardInicioService;
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
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardInicioService dashboardInicioService;

    @Operation(summary = "Dashboard da tela Início, separado por financeiro (com filtro de produto) e cadastros")
    @GetMapping("/inicio")
    public ResponseEntity<DashboardInicioResponse> inicio(
            @RequestParam(name = "meses", defaultValue = "12") int meses,
            @RequestParam(name = "tipo_produto", defaultValue = "TODOS") String tipoProduto,
            @RequestParam(name = "limite_atividades", defaultValue = "10") int limiteAtividades,
            @RequestParam(name = "limite_alertas", defaultValue = "10") int limiteAlertas,
            @RequestParam(name = "limite_tops", defaultValue = "5") int limiteTops) {
        return ResponseEntity.ok(dashboardInicioService.obterDashboard(
                meses, tipoProduto, limiteAtividades, limiteAlertas, limiteTops));
    }
}
