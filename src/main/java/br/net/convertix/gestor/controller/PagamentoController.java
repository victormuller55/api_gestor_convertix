package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.request.EstornoPagamentoRequest;
import br.net.convertix.gestor.dto.request.PagamentoCartaoRequest;
import br.net.convertix.gestor.dto.request.PagamentoPixRequest;
import br.net.convertix.gestor.dto.request.PagamentoRequest;
import br.net.convertix.gestor.dto.response.PageResponse;
import br.net.convertix.gestor.dto.response.PagamentoResponse;
import br.net.convertix.gestor.dto.response.PagamentoResumoResponse;
import br.net.convertix.gestor.enums.FormaPagamento;
import br.net.convertix.gestor.enums.StatusPagamento;
import br.net.convertix.gestor.service.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pagamentos")
@RequiredArgsConstructor
@Tag(name = "Pagamentos")
@SecurityRequirement(name = "bearerAuth")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @Operation(summary = "Criar cobrança (cliente escolhe a forma ao pagar)")
    @PostMapping
    public ResponseEntity<PagamentoResponse> criar(@Valid @RequestBody PagamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagamentoService.criar(request));
    }

    @Operation(summary = "Criar cobrança PIX")
    @PostMapping("/pix")
    public ResponseEntity<PagamentoResponse> criarPix(@Valid @RequestBody PagamentoPixRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagamentoService.criarPix(request));
    }

    @Operation(summary = "Criar pagamento com cartão de crédito")
    @PostMapping("/cartao")
    public ResponseEntity<PagamentoResponse> criarCartao(
            @Valid @RequestBody PagamentoCartaoRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagamentoService.criarCartao(request, httpRequest));
    }

    @Operation(summary = "Listar pagamentos com filtros e paginação")
    @GetMapping
    public ResponseEntity<PageResponse<PagamentoResponse>> listar(
            @Parameter(description = "Status do pagamento")
            @RequestParam(required = false) StatusPagamento status,
            @Parameter(description = "Forma de pagamento")
            @RequestParam(required = false, name = "forma_pagamento") FormaPagamento formaPagamento,
            @Parameter(description = "Data inicial (yyyy-MM-dd)")
            @RequestParam(required = false, name = "data_inicio")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @Parameter(description = "Data final (yyyy-MM-dd)")
            @RequestParam(required = false, name = "data_fim")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(pagamentoService.listar(status, formaPagamento, dataInicio, dataFim, page, size));
    }

    @Operation(summary = "Últimos pagamentos do usuário autenticado")
    @GetMapping("/ultimos")
    public ResponseEntity<List<PagamentoResumoResponse>> ultimos() {
        return ResponseEntity.ok(pagamentoService.listarUltimos());
    }

    @Operation(summary = "Histórico completo de pagamentos (tela de histórico)")
    @GetMapping("/historico")
    public ResponseEntity<List<PagamentoResponse>> historico() {
        return ResponseEntity.ok(pagamentoService.historicoCompleto());
    }

    @Operation(summary = "Consultar status atualizado no Asaas e sincronizar")
    @GetMapping("/status/{id}")
    public ResponseEntity<PagamentoResponse> sincronizarStatus(@PathVariable Long id) {
        return ResponseEntity.ok(pagamentoService.sincronizarStatus(id));
    }

    @Operation(summary = "Detalhes completos do pagamento")
    @GetMapping("/{id}")
    public ResponseEntity<PagamentoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pagamentoService.buscarPorId(id));
    }

    @Operation(summary = "Cancelar pagamento quando permitido")
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<PagamentoResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(pagamentoService.cancelar(id));
    }

    @Operation(summary = "Solicitar estorno do pagamento")
    @PostMapping("/{id}/estornar")
    public ResponseEntity<PagamentoResponse> estornar(
            @PathVariable Long id,
            @RequestBody(required = false) EstornoPagamentoRequest request) {
        return ResponseEntity.ok(pagamentoService.estornar(id, request));
    }
}
