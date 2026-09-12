package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.request.ProjetoEtapaRequest;
import br.net.convertix.gestor.dto.request.ProjetoRequest;
import br.net.convertix.gestor.dto.response.PageResponse;
import br.net.convertix.gestor.dto.response.ProjetoResponse;
import br.net.convertix.gestor.enums.EtapaProjeto;
import br.net.convertix.gestor.enums.TipoProjeto;
import br.net.convertix.gestor.service.ProjetoService;
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
@RequestMapping("/api/v1/projetos")
@RequiredArgsConstructor
@Tag(name = "Projetos")
public class ProjetoController {

    private final ProjetoService projetoService;

    @Operation(summary = "Listar projetos ou buscar por filtros (paginado)")
    @GetMapping
    public ResponseEntity<PageResponse<ProjetoResponse>> buscar(
            @Parameter(description = "Filtrar por ID do projeto")
            @RequestParam(required = false) Long id,
            @Parameter(description = "Busca parcial por título, descrição ou cliente")
            @RequestParam(required = false) String query,
            @Parameter(description = "Filtrar por ID do cliente")
            @RequestParam(required = false, name = "cliente_id") Long clienteId,
            @Parameter(description = "Filtrar por etapa")
            @RequestParam(required = false) EtapaProjeto etapa,
            @Parameter(description = "Filtrar por tipo")
            @RequestParam(required = false) TipoProjeto tipo,
            @Parameter(description = "Número da página (base 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (padrão 30, máximo 100)")
            @RequestParam(defaultValue = "" + PaginationUtil.DEFAULT_SIZE) int size) {
        return ResponseEntity.ok(projetoService.buscar(id, query, clienteId, etapa, tipo, page, size));
    }

    @Operation(summary = "Cadastrar novo projeto")
    @PostMapping("/novo")
    public ResponseEntity<ProjetoResponse> criar(@Valid @RequestBody ProjetoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projetoService.criar(request));
    }

    @Operation(summary = "Alterar dados do projeto")
    @PutMapping("/alterar-dados")
    public ResponseEntity<ProjetoResponse> atualizar(
            @RequestParam Long id,
            @Valid @RequestBody ProjetoRequest request) {
        return ResponseEntity.ok(projetoService.atualizar(id, request));
    }

    @Operation(summary = "Alterar somente a etapa do projeto")
    @PutMapping("/alterar-etapa")
    public ResponseEntity<ProjetoResponse> alterarEtapa(
            @RequestParam Long id,
            @Valid @RequestBody ProjetoEtapaRequest request) {
        return ResponseEntity.ok(projetoService.alterarEtapa(id, request));
    }

    @Operation(summary = "Apagar projeto")
    @DeleteMapping("/apagar")
    public ResponseEntity<Void> excluir(@RequestParam Long id) {
        projetoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
