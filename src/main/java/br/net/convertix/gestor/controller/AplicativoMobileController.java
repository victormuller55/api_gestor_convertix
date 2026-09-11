package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.request.AplicativoMobileRequest;
import br.net.convertix.gestor.dto.response.AplicativoMobileResponse;
import br.net.convertix.gestor.dto.response.PageResponse;
import br.net.convertix.gestor.enums.StatusAplicativoMobile;
import br.net.convertix.gestor.service.AplicativoMobileService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/aplicativos-mobile")
@RequiredArgsConstructor
@Tag(name = "Aplicativo Mobile")
public class AplicativoMobileController {

    private final AplicativoMobileService aplicativoMobileService;

    @Operation(summary = "Listar aplicativos mobile ou buscar por filtros (paginado)")
    @GetMapping
    public ResponseEntity<PageResponse<AplicativoMobileResponse>> buscar(
            @Parameter(description = "Filtrar por ID do aplicativo")
            @RequestParam(required = false) Long id,
            @Parameter(description = "Busca parcial por nome, descrição, package Android ou Bundle ID")
            @RequestParam(required = false) String query,
            @Parameter(description = "Filtrar por ID do cliente")
            @RequestParam(required = false, name = "cliente_id") Long clienteId,
            @Parameter(description = "Filtrar por status")
            @RequestParam(required = false) StatusAplicativoMobile status,
            @Parameter(description = "Número da página (base 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (padrão 30, máximo 100)")
            @RequestParam(defaultValue = "" + PaginationUtil.DEFAULT_SIZE) int size) {
        return ResponseEntity.ok(aplicativoMobileService.buscar(id, query, clienteId, status, page, size));
    }

    @Operation(summary = "Cadastrar novo aplicativo mobile")
    @PostMapping("/novo")
    public ResponseEntity<AplicativoMobileResponse> criar(@Valid @RequestBody AplicativoMobileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aplicativoMobileService.criar(request));
    }

    @Operation(summary = "Alterar dados do aplicativo mobile")
    @PutMapping("/alterar-dados")
    public ResponseEntity<AplicativoMobileResponse> atualizar(
            @RequestParam Long id,
            @Valid @RequestBody AplicativoMobileRequest request) {
        return ResponseEntity.ok(aplicativoMobileService.atualizar(id, request));
    }

    @Operation(summary = "Apagar aplicativo mobile")
    @DeleteMapping("/apagar")
    public ResponseEntity<Void> excluir(@RequestParam Long id) {
        aplicativoMobileService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Enviar ou substituir o documento de requisitos (PDF)")
    @PostMapping(value = "/documento-requisitos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AplicativoMobileResponse> enviarDocumento(
            @RequestParam Long id,
            @RequestPart("documento") MultipartFile documento) {
        return ResponseEntity.ok(aplicativoMobileService.enviarDocumento(id, documento));
    }

    @Operation(summary = "Remover o documento de requisitos")
    @DeleteMapping("/documento-requisitos")
    public ResponseEntity<AplicativoMobileResponse> removerDocumento(@RequestParam Long id) {
        return ResponseEntity.ok(aplicativoMobileService.removerDocumento(id));
    }
}
