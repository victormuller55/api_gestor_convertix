package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.request.LandingPageLeadRequest;
import br.net.convertix.gestor.dto.response.LandingPageLeadResponse;
import br.net.convertix.gestor.dto.response.LandingPagePublicoResponse;
import br.net.convertix.gestor.service.LandingPageLeadService;
import br.net.convertix.gestor.service.LandingPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/landing-pages/publico")
@RequiredArgsConstructor
@Tag(name = "Landing Page Pública")
public class LandingPagePublicoController {

    private final LandingPageService landingPageService;
    private final LandingPageLeadService landingPageLeadService;

    @Operation(summary = "Consultar landing page publicada pelo slug")
    @GetMapping
    public ResponseEntity<LandingPagePublicoResponse> buscar(
            @Parameter(description = "Slug público da landing page", required = true)
            @RequestParam String slug) {
        return ResponseEntity.ok(landingPageService.buscarPublicoPorSlug(slug));
    }

    @Operation(summary = "Receber envio de formulário")
    @PostMapping("/leads")
    public ResponseEntity<LandingPageLeadResponse> receberLead(
            @RequestParam String slug,
            @Valid @RequestBody LandingPageLeadRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(landingPageLeadService.receberEnvio(
                slug,
                request,
                resolverIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                resolverOrigem(httpRequest)));
    }

    private String resolverIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolverOrigem(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (StringUtils.hasText(origin)) {
            return origin;
        }
        return request.getHeader("Referer");
    }
}
