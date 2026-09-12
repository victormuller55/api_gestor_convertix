package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.request.PlanoRequest;
import br.net.convertix.gestor.dto.response.PageResponse;
import br.net.convertix.gestor.dto.response.PlanoResponse;
import br.net.convertix.gestor.enums.TipoProjeto;
import br.net.convertix.gestor.service.PlanoService;
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
@RequestMapping("/api/v1/planos")
@RequiredArgsConstructor
@Tag(name = "Planos")
public class PlanoController {

    private final PlanoService planoService;

    @Operation(summary = "Listar planos do catálogo (paginado)")
    @GetMapping
    public ResponseEntity<PageResponse<PlanoResponse>> buscar(
            @Parameter(description = "Busca parcial por nome, código ou descrição")
            @RequestParam(required = false) String query,
            @Parameter(description = "Filtrar por tipo de produto")
            @RequestParam(required = false) TipoProjeto tipo,
            @Parameter(description = "Filtrar por ativo")
            @RequestParam(required = false) Boolean ativo,
            @Parameter(description = "Número da página (base 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (padrão 30, máximo 100)")
            @RequestParam(defaultValue = "" + PaginationUtil.DEFAULT_SIZE) int size) {
        return ResponseEntity.ok(planoService.buscar(query, tipo, ativo, page, size));
    }

    @Operation(summary = "Cadastrar novo plano")
    @PostMapping("/novo")
    public ResponseEntity<PlanoResponse> criar(@Valid @RequestBody PlanoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planoService.criar(request));
    }

    @Operation(summary = "Alterar dados do plano")
    @PutMapping("/alterar-dados")
    public ResponseEntity<PlanoResponse> atualizar(
            @RequestParam Long id,
            @Valid @RequestBody PlanoRequest request) {
        return ResponseEntity.ok(planoService.atualizar(id, request));
    }

    @Operation(summary = "Apagar plano")
    @DeleteMapping("/apagar")
    public ResponseEntity<Void> excluir(@RequestParam Long id) {
        planoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
