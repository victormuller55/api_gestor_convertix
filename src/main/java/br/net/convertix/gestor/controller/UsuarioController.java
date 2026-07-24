package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.request.UsuarioRequest;
import br.net.convertix.gestor.dto.response.PageResponse;
import br.net.convertix.gestor.dto.response.UsuarioResponse;
import br.net.convertix.gestor.service.UsuarioService;
import br.net.convertix.gestor.util.PaginationUtil;
import br.net.convertix.gestor.validation.ValidationGroups;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Usuario")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Listar usuários ou buscar por filtros (paginado)")
    @GetMapping
    public ResponseEntity<PageResponse<UsuarioResponse>> buscar(
            @Parameter(description = "Filtrar por ID do usuário")
            @RequestParam(required = false) Long id,
            @Parameter(description = "Busca parcial por nome ou email")
            @RequestParam(required = false) String query,
            @Parameter(description = "Filtrar por status ativo (true ou false)")
            @RequestParam(required = false) Boolean ativo,
            @Parameter(description = "Número da página (base 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (padrão 30, máximo 100)")
            @RequestParam(defaultValue = "" + PaginationUtil.DEFAULT_SIZE) int size) {
        return ResponseEntity.ok(usuarioService.buscar(id, query, ativo, page, size));
    }

    @Operation(summary = "Cadastrar novo usuário admin")
    @PostMapping(value = "/novo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UsuarioResponse> criar(
            @RequestPart("dados") @Validated(ValidationGroups.OnCreate.class) UsuarioRequest request,
            @RequestPart(value = "foto", required = false) MultipartFile foto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.criar(request, foto));
    }

    @Operation(summary = "Alterar dados do usuário")
    @PutMapping(value = "/alterar-dados", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UsuarioResponse> atualizar(
            @RequestParam Long id,
            @RequestPart("dados") @Validated(ValidationGroups.OnUpdate.class) UsuarioRequest request,
            @RequestPart(value = "foto", required = false) MultipartFile foto) {
        return ResponseEntity.ok(usuarioService.atualizar(id, request, foto));
    }

    @Operation(summary = "Apagar usuário")
    @DeleteMapping("/apagar")
    public ResponseEntity<Void> excluir(@RequestParam Long id) {
        usuarioService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
