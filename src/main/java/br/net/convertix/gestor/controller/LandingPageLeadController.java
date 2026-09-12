package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.request.LandingPageLeadStatusRequest;
import br.net.convertix.gestor.dto.response.LandingPageLeadResponse;
import br.net.convertix.gestor.enums.StatusLandingPageLead;
import br.net.convertix.gestor.service.LandingPageLeadService;
import br.net.convertix.gestor.util.PaginationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/landing-pages/leads")
@RequiredArgsConstructor
@Tag(name = "Landing Page Leads")
public class LandingPageLeadController {

    private final LandingPageLeadService leadService;

    @Operation(summary = "Listar leads (paginado)")
    @GetMapping
    public ResponseEntity<?> buscar(
            @Parameter(description = "Filtrar por landing page")
            @RequestParam(required = false, name = "landing_page_id") Long landingPageId,
            @Parameter(description = "Filtrar por ID do lead")
            @RequestParam(required = false) Long id,
            @Parameter(description = "Filtrar por status")
            @RequestParam(required = false) StatusLandingPageLead status,
            @Parameter(description = "Busca por nome, e-mail ou telefone")
            @RequestParam(required = false) String query,
            @Parameter(description = "Número da página (base 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (padrão 30, máximo 100)")
            @RequestParam(defaultValue = "" + PaginationUtil.DEFAULT_SIZE) int size) {
        if (id != null) {
            if (landingPageId == null) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(leadService.buscarPorId(landingPageId, id));
        }
        return ResponseEntity.ok(leadService.listar(landingPageId, status, query, page, size));
    }

    @Operation(summary = "Alterar status do lead")
    @PutMapping("/alterar-status")
    public ResponseEntity<LandingPageLeadResponse> alterarStatus(
            @RequestParam(name = "landing_page_id") Long landingPageId,
            @RequestParam Long id,
            @Valid @RequestBody LandingPageLeadStatusRequest request) {
        return ResponseEntity.ok(leadService.alterarStatus(landingPageId, id, request));
    }

    @Operation(summary = "Apagar lead")
    @DeleteMapping("/apagar")
    public ResponseEntity<Void> excluir(
            @RequestParam(name = "landing_page_id") Long landingPageId,
            @RequestParam Long id) {
        leadService.excluir(landingPageId, id);
        return ResponseEntity.noContent().build();
    }
}
