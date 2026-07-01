package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.request.ClienteRequest;
import br.net.convertix.gestor.dto.response.ClienteResponse;
import br.net.convertix.gestor.service.ClienteService;
import br.net.convertix.gestor.validation.ValidationGroups;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@Tag(name = "Cliente")
public class ClienteController {

    private final ClienteService clienteService;

    @Operation(summary = "Listar clientes ou buscar por filtros")
    @GetMapping
    public ResponseEntity<List<ClienteResponse>> buscar(
            @Parameter(description = "Filtrar por ID do cliente")
            @RequestParam(required = false) Long id,
            @Parameter(description = "Busca parcial por nome da empresa, documento, email ou telefone")
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(clienteService.buscar(id, query));
    }

    @Operation(summary = "Cadastrar novo cliente")
    @PostMapping(value = "/novo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClienteResponse> criar(
            @RequestPart("dados") @Validated(ValidationGroups.OnCreate.class) ClienteRequest request,
            @RequestPart(value = "foto", required = false) MultipartFile foto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.criar(request, foto));
    }

    @Operation(summary = "Alterar dados do cliente")
    @PutMapping(value = "/alterar-dados", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClienteResponse> atualizar(
            @RequestParam Long id,
            @RequestPart("dados") @Validated(ValidationGroups.OnUpdate.class) ClienteRequest request,
            @RequestPart(value = "foto", required = false) MultipartFile foto) {
        return ResponseEntity.ok(clienteService.atualizar(id, request, foto));
    }

    @Operation(summary = "Apagar cliente")
    @DeleteMapping("/apagar")
    public ResponseEntity<Void> excluir(@RequestParam Long id) {
        clienteService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
