package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.request.AssinaturaRequest;
import br.net.convertix.gestor.dto.request.AssinaturaUpdateRequest;
import br.net.convertix.gestor.dto.response.AssinaturaResponse;
import br.net.convertix.gestor.enums.StatusAssinatura;
import br.net.convertix.gestor.service.AssinaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assinaturas")
@RequiredArgsConstructor
@Tag(name = "Assinaturas")
@SecurityRequirement(name = "bearerAuth")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;

    @Operation(summary = "Criar assinatura")
    @PostMapping
    public ResponseEntity<AssinaturaResponse> criar(
            @Valid @RequestBody AssinaturaRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assinaturaService.criar(request, httpRequest));
    }

    @Operation(summary = "Listar assinaturas")
    @GetMapping
    public ResponseEntity<List<AssinaturaResponse>> listar(
            @Parameter(description = "Filtrar por status")
            @RequestParam(required = false) StatusAssinatura status) {
        return ResponseEntity.ok(assinaturaService.listar(status));
    }

    @Operation(summary = "Consultar detalhes da assinatura (inclui histórico de cobranças e próxima cobrança)")
    @GetMapping("/{id}")
    public ResponseEntity<AssinaturaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(assinaturaService.buscarPorId(id));
    }

    @Operation(summary = "Atualizar assinatura")
    @PutMapping("/{id}")
    public ResponseEntity<AssinaturaResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AssinaturaUpdateRequest request) {
        return ResponseEntity.ok(assinaturaService.atualizar(id, request));
    }

    @Operation(summary = "Cancelar assinatura")
    @DeleteMapping("/{id}")
    public ResponseEntity<AssinaturaResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(assinaturaService.cancelar(id));
    }
}
