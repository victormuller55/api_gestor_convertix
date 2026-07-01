package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.request.BioLinkItemRequest;
import br.net.convertix.gestor.dto.response.BioLinkItemResponse;
import br.net.convertix.gestor.service.BioLinkItemService;
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
@RequestMapping("/api/v1/biolinks/itens")
@RequiredArgsConstructor
@Tag(name = "BioLink Item")
public class BioLinkItemController {

    private final BioLinkItemService bioLinkItemService;

    @Operation(summary = "Listar itens ou buscar por filtros")
    @GetMapping
    public ResponseEntity<?> buscar(
            @RequestParam(name = "biolink_id") Long biolinkId,
            @RequestParam(required = false) Long id) {
        if (id != null) {
            return ResponseEntity.ok(bioLinkItemService.buscarPorId(biolinkId, id));
        }
        return ResponseEntity.ok(bioLinkItemService.listarPorBioLink(biolinkId));
    }

    @Operation(summary = "Cadastrar novo item")
    @PostMapping("/novo")
    public ResponseEntity<BioLinkItemResponse> criar(@Valid @RequestBody BioLinkItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bioLinkItemService.criar(request.getBiolinkId(), request));
    }

    @Operation(summary = "Alterar dados do item")
    @PutMapping("/alterar-dados")
    public ResponseEntity<BioLinkItemResponse> atualizar(
            @RequestParam(name = "biolink_id") Long biolinkId,
            @RequestParam Long id,
            @Valid @RequestBody BioLinkItemRequest request) {
        return ResponseEntity.ok(bioLinkItemService.atualizar(biolinkId, id, request));
    }

    @Operation(summary = "Apagar item")
    @DeleteMapping("/apagar")
    public ResponseEntity<Void> excluir(
            @RequestParam(name = "biolink_id") Long biolinkId,
            @RequestParam Long id) {
        bioLinkItemService.excluir(biolinkId, id);
        return ResponseEntity.noContent().build();
    }
}
