package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.request.BioLinkRequest;
import br.net.convertix.gestor.dto.response.BioLinkResponse;
import br.net.convertix.gestor.dto.response.PageResponse;
import br.net.convertix.gestor.service.BioLinkService;
import br.net.convertix.gestor.util.PaginationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/biolinks")
@RequiredArgsConstructor
@Tag(name = "BioLink")
public class BioLinkController {

    private final BioLinkService bioLinkService;

    @Operation(summary = "Listar biolinks ou buscar por filtros (paginado)")
    @GetMapping
    public ResponseEntity<PageResponse<BioLinkResponse>> buscar(
            @Parameter(description = "Filtrar por ID do biolink")
            @RequestParam(required = false) Long id,
            @Parameter(description = "Número da página (base 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (padrão 30, máximo 100)")
            @RequestParam(defaultValue = "" + PaginationUtil.DEFAULT_SIZE) int size) {
        return ResponseEntity.ok(bioLinkService.buscar(id, page, size));
    }

    @Operation(summary = "Cadastrar novo biolink")
    @PostMapping(value = "/novo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BioLinkResponse> criar(
            @RequestPart("dados") @Valid BioLinkRequest request,
            @RequestPart(value = "foto", required = false) MultipartFile foto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bioLinkService.criar(request, foto));
    }

    @Operation(summary = "Alterar dados do biolink")
    @PutMapping(value = "/alterar-dados", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BioLinkResponse> atualizar(
            @RequestParam Long id,
            @RequestPart("dados") @Valid BioLinkRequest request,
            @RequestPart(value = "foto", required = false) MultipartFile foto) {
        return ResponseEntity.ok(bioLinkService.atualizar(id, request, foto));
    }

    @Operation(summary = "Apagar biolink")
    @DeleteMapping("/apagar")
    public ResponseEntity<Void> excluir(@RequestParam Long id) {
        bioLinkService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
