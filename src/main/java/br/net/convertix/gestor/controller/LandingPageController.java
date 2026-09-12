package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.request.LandingPageRequest;
import br.net.convertix.gestor.dto.response.LandingPageResponse;
import br.net.convertix.gestor.dto.response.PageResponse;
import br.net.convertix.gestor.service.LandingPageService;
import br.net.convertix.gestor.util.PaginationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/landing-pages")
@RequiredArgsConstructor
@Tag(name = "Landing Pages")
public class LandingPageController {

    private final LandingPageService landingPageService;

    @Operation(summary = "Listar landing pages (paginado)")
    @GetMapping
    public ResponseEntity<PageResponse<LandingPageResponse>> buscar(
            @Parameter(description = "Filtrar por ID")
            @RequestParam(required = false) Long id,
            @Parameter(description = "Busca por slug, site ou cliente")
            @RequestParam(required = false) String query,
            @Parameter(description = "Número da página (base 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (padrão 30, máximo 100)")
            @RequestParam(defaultValue = "" + PaginationUtil.DEFAULT_SIZE) int size) {
        return ResponseEntity.ok(landingPageService.buscar(id, query, page, size));
    }

    @Operation(summary = "Cadastrar landing page")
    @PostMapping("/novo")
    public ResponseEntity<LandingPageResponse> criar(@Valid @RequestBody LandingPageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(landingPageService.criar(request));
    }

    @Operation(summary = "Alterar landing page")
    @PutMapping("/alterar-dados")
    public ResponseEntity<LandingPageResponse> atualizar(
            @RequestParam Long id,
            @Valid @RequestBody LandingPageRequest request) {
        return ResponseEntity.ok(landingPageService.atualizar(id, request));
    }

    @Operation(summary = "Apagar landing page")
    @DeleteMapping("/apagar")
    public ResponseEntity<Void> excluir(@RequestParam Long id) {
        landingPageService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
