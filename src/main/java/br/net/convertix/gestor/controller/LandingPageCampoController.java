package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.request.LandingPageCampoRequest;
import br.net.convertix.gestor.dto.response.LandingPageCampoResponse;
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
@RequestMapping("/api/v1/landing-pages/campos")
@RequiredArgsConstructor
@Tag(name = "Landing Page Campos")
public class LandingPageCampoController {

    private final LandingPageFormularioService formularioService;

    @Operation(summary = "Listar campos do formulário")
    @GetMapping
    public ResponseEntity<?> buscar(@RequestParam(name = "formulario_id") Long formularioId) {
        return ResponseEntity.ok(formularioService.listarCampos(formularioId));
    }

    @Operation(summary = "Cadastrar campo")
    @PostMapping("/novo")
    public ResponseEntity<LandingPageCampoResponse> criar(
            @RequestParam(name = "formulario_id") Long formularioId,
            @Valid @RequestBody LandingPageCampoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(formularioService.adicionarCampo(formularioId, request));
    }

    @Operation(summary = "Alterar campo")
    @PutMapping("/alterar-dados")
    public ResponseEntity<LandingPageCampoResponse> atualizar(
            @RequestParam Long id,
            @Valid @RequestBody LandingPageCampoRequest request) {
        return ResponseEntity.ok(formularioService.editarCampo(id, request));
    }

    @Operation(summary = "Apagar campo")
    @DeleteMapping("/apagar")
    public ResponseEntity<Void> excluir(@RequestParam Long id) {
        formularioService.removerCampo(id);
        return ResponseEntity.noContent().build();
    }
}
