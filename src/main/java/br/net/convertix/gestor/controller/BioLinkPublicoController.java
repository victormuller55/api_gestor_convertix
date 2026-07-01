package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.response.BioLinkPublicoResponse;
import br.net.convertix.gestor.service.BioLinkPublicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/biolinks/publico")
@RequiredArgsConstructor
@Tag(name = "BioLink Público")
public class BioLinkPublicoController {

    private final BioLinkPublicoService bioLinkPublicoService;

    @Operation(summary = "Consultar BioLink publicado pelo ID do site")
    @GetMapping
    public ResponseEntity<BioLinkPublicoResponse> buscarPorSite(
            @Parameter(description = "ID do site no banco de dados", required = true)
            @RequestParam(name = "site_id") Long siteId) {
        return ResponseEntity.ok(bioLinkPublicoService.buscarPorSiteId(siteId));
    }
}
