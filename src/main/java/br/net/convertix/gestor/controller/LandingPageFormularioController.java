package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.request.LandingPageFormularioRequest;
import br.net.convertix.gestor.dto.response.LandingPageFormularioResponse;
import br.net.convertix.gestor.service.LandingPageFormularioService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/landing-pages/formularios")
@RequiredArgsConstructor
@Tag(name = "Landing Page Formulários")
public class LandingPageFormularioController {

    private final LandingPageFormularioService formularioService;

    @Operation(summary = "Listar formulários da landing page")
    @GetMapping
    public ResponseEntity<?> buscar(
            @RequestParam(name = "landing_page_id") Long landingPageId,
            @RequestParam(required = false) Long id) {
        if (id != null) {
            return ResponseEntity.ok(formularioService.buscarPorId(landingPageId, id));
        }
        return ResponseEntity.ok(formularioService.listarPorLandingPage(landingPageId));
    }

    @Operation(summary = "Cadastrar formulário")
    @PostMapping("/novo")
    public ResponseEntity<LandingPageFormularioResponse> criar(
            @RequestParam(name = "landing_page_id") Long landingPageId,
            @Valid @RequestBody LandingPageFormularioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(formularioService.criar(landingPageId, request));
    }

    @Operation(summary = "Alterar formulário")
    @PutMapping("/alterar-dados")
    public ResponseEntity<LandingPageFormularioResponse> atualizar(
            @RequestParam(name = "landing_page_id") Long landingPageId,
            @RequestParam Long id,
            @Valid @RequestBody LandingPageFormularioRequest request) {
        return ResponseEntity.ok(formularioService.editar(landingPageId, id, request));
    }

    @Operation(summary = "Apagar formulário")
    @DeleteMapping("/apagar")
    public ResponseEntity<Void> excluir(
            @RequestParam(name = "landing_page_id") Long landingPageId,
            @RequestParam Long id) {
        formularioService.excluir(landingPageId, id);
        return ResponseEntity.noContent().build();
    }
}
