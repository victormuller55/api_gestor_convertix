package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.request.SiteRequest;
import br.net.convertix.gestor.dto.response.PageResponse;
import br.net.convertix.gestor.dto.response.SiteResponse;
import br.net.convertix.gestor.service.SiteService;
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
@RequestMapping("/api/v1/sites")
@RequiredArgsConstructor
@Tag(name = "Site")
public class SiteController {

    private final SiteService siteService;

    @Operation(summary = "Listar sites ou buscar por filtros (paginado)")
    @GetMapping
    public ResponseEntity<PageResponse<SiteResponse>> buscar(
            @Parameter(description = "Filtrar por ID do site")
            @RequestParam(required = false) Long id,
            @Parameter(description = "Busca parcial por nome, domínio ou subdomínio")
            @RequestParam(required = false) String query,
            @Parameter(description = "Número da página (base 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (padrão 30, máximo 100)")
            @RequestParam(defaultValue = "" + PaginationUtil.DEFAULT_SIZE) int size) {
        return ResponseEntity.ok(siteService.buscar(id, query, page, size));
    }

    @Operation(summary = "Cadastrar novo site")
    @PostMapping("/novo")
    public ResponseEntity<SiteResponse> criar(@Valid @RequestBody SiteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siteService.criar(request));
    }

    @Operation(summary = "Alterar dados do site")
    @PutMapping("/alterar-dados")
    public ResponseEntity<SiteResponse> atualizar(
            @RequestParam Long id,
            @Valid @RequestBody SiteRequest request) {
        return ResponseEntity.ok(siteService.atualizar(id, request));
    }

    @Operation(summary = "Apagar site")
    @DeleteMapping("/apagar")
    public ResponseEntity<Void> excluir(@RequestParam Long id) {
        siteService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
